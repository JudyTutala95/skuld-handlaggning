package se.jtutala.handlaggning.galdenar;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import se.jtutala.handlaggning.galdenar.dto.GaldenarResponse;
import se.jtutala.handlaggning.galdenar.dto.RegistreraGaldenarRequest;
import se.jtutala.handlaggning.galdenar.dto.SokGaldenarRequest;
import se.jtutala.handlaggning.galdenar.dto.UppdateraEkonomiRequest;
import se.jtutala.handlaggning.galdenar.regelverk.Forbehallsberakning;

/// Hela gäldenärstjänsten på riktigt: HTTP → controller → service → domän → databas.
///
/// Testar också **API:t som ärendetjänsten är beroende av**: `GET /api/galdenarer/{id}` och
/// `GET /api/galdenarer/{id}/forbehallsbelopp`. Ändras svaren här måste ärendetjänsten klara det.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class GaldenarApiIntegrationTest {

    @Autowired
    RestTestClient http;

    @Test
    void registreraGaldenarOchRaknaUtForbehallsbelopp() {
        GaldenarResponse dana = post("/api/galdenarer",
                new RegistreraGaldenarRequest("20010923-2397", "Dana", "Prov", "Provgatan 5, 555 55 Provstad"),
                HttpStatus.CREATED, GaldenarResponse.class);
        http.put().uri("/api/galdenarer/{id}/ekonomi", dana.id()).contentType(MediaType.APPLICATION_JSON)
                .body(new UppdateraEkonomiRequest(new BigDecimal("25000"), new BigDecimal("8000"), false, List.of()))
                .exchange().expectStatus().isOk();

        Forbehallsberakning berakning = http.get().uri("/api/galdenarer/{id}/forbehallsbelopp", dana.id())
                .exchange().expectStatus().isOk()
                .expectBody(Forbehallsberakning.class).returnResult().getResponseBody();

        assertThat(berakning).isNotNull();
        assertThat(berakning.utmatningsbartBelopp()).isEqualByComparingTo("10757"); // 25 000 − (6 243 + 8 000)
    }

    @Test
    void okandGaldenarGer404SaAttArendetjanstenKanSvara404() {
        http.get().uri("/api/galdenarer/999").exchange().expectStatus().isNotFound();
        http.get().uri("/api/galdenarer/999/forbehallsbelopp").exchange().expectStatus().isNotFound();
    }

    @Test
    void exempeldataFinnsOchSokningSkerMedPostSaPersonnummerInteHamnarIUrl() {
        GaldenarResponse anna = post("/api/galdenarer/sok", new SokGaldenarRequest("19850315-2384"),
                HttpStatus.OK, GaldenarResponse.class);

        assertThat(anna.namn()).isEqualTo("Anna Exempelsson");
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

    private <T> T post(String uri, Object body, HttpStatus forvantad, Class<T> typ) {
        T svar = http.post().uri(uri).contentType(MediaType.APPLICATION_JSON).body(body).exchange()
                .expectStatus().isEqualTo(forvantad).expectBody(typ).returnResult().getResponseBody();
        assertThat(svar).isNotNull();
        return svar;
    }
}
