package se.jtutala.handlaggning;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import se.jtutala.handlaggning.arende.ArendeDtos.ArendeSvar;
import se.jtutala.handlaggning.arende.ArendeDtos.Bestridande;
import se.jtutala.handlaggning.arende.ArendeDtos.Delgivning;
import se.jtutala.handlaggning.arende.ArendeDtos.RegistreraArende;
import se.jtutala.handlaggning.arende.ArendeDtos.RegistreraInbetalning;
import se.jtutala.handlaggning.arende.ArendeDtos.Verkstallighet;
import se.jtutala.handlaggning.arende.Arendestatus;
import se.jtutala.handlaggning.arende.Arendetyp;
import se.jtutala.handlaggning.galdenar.GaldenarDtos.GaldenarSvar;
import se.jtutala.handlaggning.galdenar.GaldenarDtos.RegistreraGaldenar;
import se.jtutala.handlaggning.galdenar.GaldenarDtos.SokGaldenar;
import se.jtutala.handlaggning.galdenar.GaldenarDtos.UppdateraEkonomi;
import se.jtutala.handlaggning.regelverk.Forbehallsberakning;
import se.jtutala.handlaggning.regelverk.Maltyp;

/// Hela applikationen på riktigt: HTTP → controller → service → domän → databas (H2 + Flyway).
///
/// Testet följer ett ärende genom hela kedjan, precis som i verkligheten:
/// betalningsföreläggande → delgivning → utslag → verkställighet → utredning →
/// löneutmätning → inbetalningar → betalt.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class HandlaggningIntegrationTest {

    @Autowired
    RestTestClient http;

    @Test
    void heltFlodeFranBetalningsforelaggandeTillBetaldSkuld() {
        // 1. Ny gäldenär (fiktivt testpersonnummer) och gäldenärens ekonomi
        GaldenarSvar g = post("/api/galdenarer",
                new RegistreraGaldenar("20010923-2397", "Dana", "Prov", "Provgatan 5, 555 55 Provstad"),
                HttpStatus.CREATED, GaldenarSvar.class);
        put("/api/galdenarer/" + g.id() + "/ekonomi",
                new UppdateraEkonomi(new BigDecimal("25000"), new BigDecimal("8000"), false, List.of()));

        Forbehallsberakning berakning = http.get().uri("/api/galdenarer/{id}/forbehallsbelopp", g.id())
                .exchange().expectStatus().isOk()
                .expectBody(Forbehallsberakning.class).returnResult().getResponseBody();
        // 25 000 − (6 243 + 8 000) = 10 757
        assertThat(berakning.utmatningsbartBelopp()).isEqualByComparingTo("10757");

        // 2. Borgenären ansöker om betalningsföreläggande
        ArendeSvar bf = post("/api/arenden", new RegistreraArende(Arendetyp.BETALNINGSFORELAGGANDE, Maltyp.ENSKILT,
                g.id(), "Exempel Finans AB", new BigDecimal("20000.00"), LocalDate.of(2026, 1, 1), false, "lisa"),
                HttpStatus.CREATED, ArendeSvar.class);
        assertThat(bf.arendenummer()).matches("BF-\\d{4}-\\d{6}");
        assertThat(bf.status()).isEqualTo(Arendestatus.REGISTRERAT);

        // 3. Delgivning – fristen går ut 10 dagar senare
        ArendeSvar delgivet = post("/api/arenden/" + bf.id() + "/delgivning", new Delgivning(LocalDate.of(2026, 1, 5)),
                HttpStatus.OK, ArendeSvar.class);
        assertThat(delgivet.bestridandefrist()).isEqualTo(LocalDate.of(2026, 1, 15));

        // 4. För sent bestridande avvisas
        http.post().uri("/api/arenden/{id}/bestridande", bf.id()).contentType(MediaType.APPLICATION_JSON)
                .body(new Bestridande(LocalDate.of(2026, 1, 20), "Jag håller inte med"))
                .exchange().expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody().jsonPath("$.detail").isEqualTo("Bestridandet kom in 2026-01-20, efter fristen 2026-01-15");

        // 5. Utslag och verkställighet → nytt utsökningsärende
        ArendeSvar utslag = post("/api/arenden/" + bf.id() + "/utslag", null, HttpStatus.OK, ArendeSvar.class);
        assertThat(utslag.status()).isEqualTo(Arendestatus.UTSLAG_MEDDELAT);

        ArendeSvar uts = post("/api/arenden/" + bf.id() + "/verkstallighet", new Verkstallighet("omar"),
                HttpStatus.CREATED, ArendeSvar.class);
        assertThat(uts.typ()).isEqualTo(Arendetyp.UTSOKNING);
        assertThat(uts.ursprungsarendeId()).isEqualTo(bf.id());

        // 6. Utredning och löneutmätning – beloppet kommer från regelverket
        post("/api/arenden/" + uts.id() + "/utredning", null, HttpStatus.OK, ArendeSvar.class);
        ArendeSvar utmatning = post("/api/arenden/" + uts.id() + "/loneutmatning", null, HttpStatus.OK, ArendeSvar.class);
        assertThat(utmatning.status()).isEqualTo(Arendestatus.LONEUTMATNING);
        assertThat(utmatning.loneutmatningPerManad()).isEqualByComparingTo("10757");

        // 7. Två inbetalningar → skulden är betald
        post("/api/arenden/" + uts.id() + "/inbetalningar",
                new RegistreraInbetalning(new BigDecimal("10757.00"), LocalDate.of(2026, 2, 25)), HttpStatus.OK, ArendeSvar.class);
        ArendeSvar betalt = post("/api/arenden/" + uts.id() + "/inbetalningar",
                new RegistreraInbetalning(new BigDecimal("9243.00"), LocalDate.of(2026, 3, 25)), HttpStatus.OK, ArendeSvar.class);

        assertThat(betalt.status()).isEqualTo(Arendestatus.BETALT);
        assertThat(betalt.kvarstaendeSkuld()).isEqualByComparingTo("0");
        assertThat(betalt.mojligaNastaStatusar()).isEmpty();
        assertThat(betalt.dagbok()).extracting(d -> d.handelse())
                .anyMatch(h -> h.startsWith("Beslut om löneutmätning"))
                .last().isEqualTo("Skulden är fullt betald.");

        // 8. Arbetslistan per gäldenär
        http.get().uri("/api/arenden?galdenarId={id}", g.id()).exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.length()").isEqualTo(2);
    }

    @Test
    void exempeldataFinnsOchSokningSkerMedPostSaPersonnummerInteHamnarIUrl() {
        GaldenarSvar anna = post("/api/galdenarer/sok", new SokGaldenar("19850315-2384"), HttpStatus.OK, GaldenarSvar.class);

        assertThat(anna.namn()).isEqualTo("Anna Exempelsson");
        http.get().uri("/api/arenden?status=REGISTRERAT").exchange().expectStatus().isOk()
                .expectBody().jsonPath("$[?(@.borgenar == 'Exempel Inkasso AB')]").exists();
    }

    @Test
    void dubblettPersonnummerGer409() {
        http.post().uri("/api/galdenarer").contentType(MediaType.APPLICATION_JSON)
                .body(new RegistreraGaldenar("19780211-2388", "Kopia", "Av Cecilia", "Någonstans 1"))
                .exchange().expectStatus().isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void ogiltigtPersonnummerGer400() {
        http.post().uri("/api/galdenarer").contentType(MediaType.APPLICATION_JSON)
                .body(new RegistreraGaldenar("19850315-0000", "Fel", "Nummer", "Gatan 1"))
                .exchange().expectStatus().isBadRequest()
                .expectBody().jsonPath("$.detail").isEqualTo("personnumrets kontrollsiffra stämmer inte");
    }

    @Test
    void ogiltigStatusovergangGer409() {
        // Exempelärende 3 är en skuldsanering i status REGISTRERAT – kan inte beviljas utan utredning
        http.post().uri("/api/arenden/3/skuldsanering/beslut").contentType(MediaType.APPLICATION_JSON)
                .body("""
                        { "beviljas": true, "motivering": "test" }
                        """)
                .exchange().expectStatus().isEqualTo(HttpStatus.CONFLICT);
    }

    // --- Hjälpmetoder ---

    private <T> T post(String uri, Object body, HttpStatus forvantad, Class<T> typ) {
        var request = http.post().uri(uri).contentType(MediaType.APPLICATION_JSON);
        var spec = body == null ? request.exchange() : request.body(body).exchange();
        T svar = spec.expectStatus().isEqualTo(forvantad).expectBody(typ).returnResult().getResponseBody();
        assertThat(svar).isNotNull();
        return svar;
    }

    private void put(String uri, Object body) {
        http.put().uri(uri).contentType(MediaType.APPLICATION_JSON).body(body).exchange().expectStatus().isOk();
    }
}
