package se.jtutala.handlaggning.arende;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

import se.jtutala.handlaggning.arende.dto.ArendeResponse;
import se.jtutala.handlaggning.arende.dto.BestridandeRequest;
import se.jtutala.handlaggning.arende.dto.DagboksanteckningResponse;
import se.jtutala.handlaggning.arende.dto.DelgivningRequest;
import se.jtutala.handlaggning.arende.dto.InbetalningRequest;
import se.jtutala.handlaggning.arende.dto.RegistreraArendeRequest;
import se.jtutala.handlaggning.arende.dto.VerkstallighetRequest;
import se.jtutala.handlaggning.arende.regelverk.Maltyp;

/// Hela ärendetjänsten på riktigt: HTTP → controller → service → domän → databas, plus
/// riktiga HTTP-anrop till en **låtsad gäldenärstjänst** (`LatsasGaldenartjanst`).
///
/// Testet följer ett ärende genom hela kedjan, precis som i verkligheten:
/// betalningsföreläggande → delgivning → utslag → verkställighet → utredning →
/// löneutmätning → inbetalningar → betalt.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class HeltFlodeIntegrationTest {

    /// Gäldenär 42 finns i den låtsade gäldenärstjänsten.
    private static final long GALDENAR = 42;

    static final LatsasGaldenartjanst galdenartjanst = starta();

    @DynamicPropertySource
    static void pekaPaLatsasGaldenartjanst(DynamicPropertyRegistry registry) {
        registry.add("spring.http.serviceclient.galdenar.base-url", galdenartjanst::url);
    }

    @Autowired
    RestTestClient http;

    /// Scenariot läses som en berättelse – varje steg är en egen metod nedan.
    @Test
    void heltFlodeFranBetalningsforelaggandeTillBetaldSkuld() {
        long bf = ansokOmBetalningsforelaggande();
        delgeOchAvvisaSentBestridande(bf);
        long utsokning = meddelaUtslagOchVerkstall(bf);
        utredOchBeslutaLoneutmatning(utsokning);
        betalaHelaSkulden(utsokning);
        arbetslistanVisarBadaArendena();
    }

    // ---------- Stegen i scenariot ----------

    private long ansokOmBetalningsforelaggande() {
        ArendeResponse bf = post("/api/arenden", new RegistreraArendeRequest(Arendetyp.BETALNINGSFORELAGGANDE, Maltyp.ENSKILT,
                GALDENAR, "Exempel Finans AB", new BigDecimal("20000.00"), LocalDate.of(2026, 1, 1), false, "lisa"),
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

    /// Beloppet 10 757 kr hämtas från gäldenärstjänsten över HTTP.
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

    private void arbetslistanVisarBadaArendena() {
        http.get().uri("/api/arenden?galdenarId={id}", GALDENAR).exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.length()").isEqualTo(2);
    }

    // ---------- När gäldenärstjänsten säger nej eller inte svarar ----------

    @Test
    void okandGaldenarGer404OchIngetArendeSkapas() {
        http.post().uri("/api/arenden").contentType(MediaType.APPLICATION_JSON)
                .body(new RegistreraArendeRequest(Arendetyp.UTSOKNING, Maltyp.ALLMANT, 999L, "Skatteverket",
                        new BigDecimal("500.00"), LocalDate.of(2026, 1, 1), false, "lisa"))
                .exchange().expectStatus().isNotFound()
                .expectBody().jsonPath("$.detail").isEqualTo("Gäldenär med id 999 hittades inte");
    }

    @Test
    void gerTrasigGaldenartjanst503EfterTreForsok() {
        galdenartjanst.bliTrasig(true);

        http.post().uri("/api/arenden").contentType(MediaType.APPLICATION_JSON)
                .body(new RegistreraArendeRequest(Arendetyp.UTSOKNING, Maltyp.ALLMANT, GALDENAR, "Skatteverket",
                        new BigDecimal("500.00"), LocalDate.of(2026, 1, 1), false, "lisa"))
                .exchange().expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectBody().jsonPath("$.title").isEqualTo("En tjänst svarar inte");
    }

    // ---------- Övriga tester ----------

    @Test
    void exempeldataFinns() {
        http.get().uri("/api/arenden?status=REGISTRERAT").exchange().expectStatus().isOk()
                .expectBody().jsonPath("$[?(@.borgenar == 'Exempel Inkasso AB')]").exists();
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

    @AfterEach
    void lagaGaldenartjansten() {
        galdenartjanst.bliTrasig(false);
    }

    @AfterAll
    static void stangGaldenartjansten() {
        galdenartjanst.close();
    }

    private static LatsasGaldenartjanst starta() {
        try {
            return new LatsasGaldenartjanst();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private ArendeResponse betala(long uts, String belopp, LocalDate datum) {
        return post("/api/arenden/" + uts + "/inbetalningar",
                new InbetalningRequest(new BigDecimal(belopp), datum), HttpStatus.OK, ArendeResponse.class);
    }

    private <T> T post(String uri, Object body, HttpStatus forvantad, Class<T> typ) {
        var request = http.post().uri(uri).contentType(MediaType.APPLICATION_JSON);
        var spec = body == null ? request.exchange() : request.body(body).exchange();
        T svar = spec.expectStatus().isEqualTo(forvantad).expectBody(typ).returnResult().getResponseBody();
        assertThat(svar).isNotNull();
        return svar;
    }
}
