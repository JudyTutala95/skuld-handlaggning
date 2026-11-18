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

import se.jtutala.handlaggning.arende.Arendestatus;
import se.jtutala.handlaggning.arende.Arendetyp;
import se.jtutala.handlaggning.arende.dto.DagboksanteckningResponse;
import se.jtutala.handlaggning.arende.dto.ArendeResponse;
import se.jtutala.handlaggning.arende.dto.BestridandeRequest;
import se.jtutala.handlaggning.arende.dto.DelgivningRequest;
import se.jtutala.handlaggning.arende.dto.InbetalningRequest;
import se.jtutala.handlaggning.arende.dto.RegistreraArendeRequest;
import se.jtutala.handlaggning.arende.dto.VerkstallighetRequest;
import se.jtutala.handlaggning.galdenar.dto.GaldenarResponse;
import se.jtutala.handlaggning.galdenar.dto.RegistreraGaldenarRequest;
import se.jtutala.handlaggning.galdenar.dto.SokGaldenarRequest;
import se.jtutala.handlaggning.galdenar.dto.UppdateraEkonomiRequest;
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

    /// Scenariot läses som en berättelse – varje steg är en egen metod nedan.
    @Test
    void heltFlodeFranBetalningsforelaggandeTillBetaldSkuld() {
        long galdenarId = registreraGaldenarMedLon25000();
        long bf = ansokOmBetalningsforelaggande(galdenarId);
        delgeOchAvvisaSentBestridande(bf);
        long utsokning = meddelaUtslagOchVerkstall(bf);
        utredOchBeslutaLoneutmatning(utsokning);
        betalaHelaSkulden(utsokning);
        arbetslistanVisarBadaArendena(galdenarId);
    }

    // ---------- Stegen i scenariot ----------

    private long registreraGaldenarMedLon25000() {
        GaldenarResponse g = post("/api/galdenarer",
                new RegistreraGaldenarRequest("20010923-2397", "Dana", "Prov", "Provgatan 5, 555 55 Provstad"),
                HttpStatus.CREATED, GaldenarResponse.class);
        put("/api/galdenarer/" + g.id() + "/ekonomi",
                new UppdateraEkonomiRequest(new BigDecimal("25000"), new BigDecimal("8000"), false, List.of()));

        Forbehallsberakning berakning = http.get().uri("/api/galdenarer/{id}/forbehallsbelopp", g.id())
                .exchange().expectStatus().isOk()
                .expectBody(Forbehallsberakning.class).returnResult().getResponseBody();
        assertThat(berakning.utmatningsbartBelopp()).isEqualByComparingTo("10757"); // 25 000 − (6 243 + 8 000)
        return g.id();
    }

    private long ansokOmBetalningsforelaggande(long galdenarId) {
        ArendeResponse bf = post("/api/arenden", new RegistreraArendeRequest(Arendetyp.BETALNINGSFORELAGGANDE, Maltyp.ENSKILT,
                galdenarId, "Exempel Finans AB", new BigDecimal("20000.00"), LocalDate.of(2026, 1, 1), false, "lisa"),
                HttpStatus.CREATED, ArendeResponse.class);
        assertThat(bf.arendenummer()).matches("BF-\\d{4}-\\d{6}");
        assertThat(bf.status()).isEqualTo(Arendestatus.REGISTRERAT);
        return bf.id();
    }

    private void delgeOchAvvisaSentBestridande(long bf) {
        ArendeResponse delgivet = post("/api/arenden/" + bf + "/delgivning", new DelgivningRequest(LocalDate.of(2026, 1, 5)),
                HttpStatus.OK, ArendeResponse.class);
        assertThat(delgivet.bestridandefrist()).isEqualTo(LocalDate.of(2026, 1, 15));

        http.post().uri("/api/arenden/{id}/bestridande", bf).contentType(MediaType.APPLICATION_JSON)
                .body(new BestridandeRequest(LocalDate.of(2026, 1, 20), "Jag håller inte med"))
                .exchange().expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody().jsonPath("$.detail").isEqualTo("Bestridandet kom in 2026-01-20, efter fristen 2026-01-15");
    }

    private long meddelaUtslagOchVerkstall(long bf) {
        ArendeResponse utslag = post("/api/arenden/" + bf + "/utslag", null, HttpStatus.OK, ArendeResponse.class);
        assertThat(utslag.status()).isEqualTo(Arendestatus.UTSLAG_MEDDELAT);

        ArendeResponse uts = post("/api/arenden/" + bf + "/verkstallighet", new VerkstallighetRequest("omar"),
                HttpStatus.CREATED, ArendeResponse.class);
        assertThat(uts.typ()).isEqualTo(Arendetyp.UTSOKNING);
        assertThat(uts.ursprungsarendeId()).isEqualTo(bf);
        return uts.id();
    }

    private void utredOchBeslutaLoneutmatning(long uts) {
        post("/api/arenden/" + uts + "/utredning", null, HttpStatus.OK, ArendeResponse.class);
        ArendeResponse utmatning = post("/api/arenden/" + uts + "/loneutmatning", null, HttpStatus.OK, ArendeResponse.class);

        assertThat(utmatning.status()).isEqualTo(Arendestatus.LONEUTMATNING);
        assertThat(utmatning.loneutmatningPerManad()).isEqualByComparingTo("10757");
    }

    private void betalaHelaSkulden(long uts) {
        betala(uts, "10757.00", LocalDate.of(2026, 2, 25));
        ArendeResponse betalt = betala(uts, "9243.00", LocalDate.of(2026, 3, 25));

        assertThat(betalt.status()).isEqualTo(Arendestatus.BETALT);
        assertThat(betalt.kvarstaendeSkuld()).isEqualByComparingTo("0");
        assertThat(betalt.mojligaNastaStatusar()).isEmpty();
        assertThat(betalt.dagbok()).extracting(DagboksanteckningResponse::handelse)
                .anyMatch(h -> h.startsWith("Beslut om löneutmätning"))
                .last().isEqualTo("Skulden är fullt betald.");
    }

    private void arbetslistanVisarBadaArendena(long galdenarId) {
        http.get().uri("/api/arenden?galdenarId={id}", galdenarId).exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.length()").isEqualTo(2);
    }

    private ArendeResponse betala(long uts, String belopp, LocalDate datum) {
        return post("/api/arenden/" + uts + "/inbetalningar",
                new InbetalningRequest(new BigDecimal(belopp), datum), HttpStatus.OK, ArendeResponse.class);
    }

    // ---------- Övriga tester ----------

    @Test
    void exempeldataFinnsOchSokningSkerMedPostSaPersonnummerInteHamnarIUrl() {
        GaldenarResponse anna = post("/api/galdenarer/sok", new SokGaldenarRequest("19850315-2384"), HttpStatus.OK, GaldenarResponse.class);

        assertThat(anna.namn()).isEqualTo("Anna Exempelsson");
        http.get().uri("/api/arenden?status=REGISTRERAT").exchange().expectStatus().isOk()
                .expectBody().jsonPath("$[?(@.borgenar == 'Exempel Inkasso AB')]").exists();
    }

    @Test
    void dubblettPersonnummerGer409() {
        http.post().uri("/api/galdenarer").contentType(MediaType.APPLICATION_JSON)
                .body(new RegistreraGaldenarRequest("19780211-2388", "Kopia", "Av Cecilia", "Någonstans 1"))
                .exchange().expectStatus().isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void ogiltigtPersonnummerGer400() {
        http.post().uri("/api/galdenarer").contentType(MediaType.APPLICATION_JSON)
                .body(new RegistreraGaldenarRequest("19850315-0000", "Fel", "Nummer", "Gatan 1"))
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
