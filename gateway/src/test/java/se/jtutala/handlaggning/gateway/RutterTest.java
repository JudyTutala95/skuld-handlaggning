package se.jtutala.handlaggning.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.sun.net.httpserver.HttpServer;

/// Startar gatewayen och två låtsade tjänster. Kontrollerar att varje sökväg hamnar hos
/// rätt tjänst och att sökvägen följer med oförändrad.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RutterTest {

    static final HttpServer galdenar = latsasTjanst("galdenar-service");
    static final HttpServer arende = latsasTjanst("arende-service");

    @DynamicPropertySource
    static void tjansternasAdresser(DynamicPropertyRegistry registry) {
        registry.add("tjanster.galdenar", () -> "http://localhost:" + galdenar.getAddress().getPort());
        registry.add("tjanster.arende", () -> "http://localhost:" + arende.getAddress().getPort());
    }

    @LocalServerPort
    int port;

    @Test
    void galdenarerGarTillGaldenartjansten() throws Exception {
        assertThat(hamta("/api/galdenarer/1/forbehallsbelopp").body())
                .isEqualTo("galdenar-service svarar på /api/galdenarer/1/forbehallsbelopp");
    }

    @Test
    void arendenGarTillArendetjansten() throws Exception {
        assertThat(hamta("/api/arenden?status=REGISTRERAT").body())
                .isEqualTo("arende-service svarar på /api/arenden");
    }

    @Test
    void okandSokvagGer404() throws Exception {
        assertThat(hamta("/api/okant").statusCode()).isEqualTo(404);
    }

    private HttpResponse<String> hamta(String sokvag) throws Exception {
        try (HttpClient klient = HttpClient.newHttpClient()) {
            return klient.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + sokvag)).build(),
                    HttpResponse.BodyHandlers.ofString());
        }
    }

    @AfterAll
    static void stang() {
        galdenar.stop(0);
        arende.stop(0);
    }

    private static HttpServer latsasTjanst(String namn) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", anrop -> {
                byte[] svar = (namn + " svarar på " + anrop.getRequestURI().getPath()).getBytes(StandardCharsets.UTF_8);
                anrop.sendResponseHeaders(200, svar.length);
                try (var ut = anrop.getResponseBody()) {
                    ut.write(svar);
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
