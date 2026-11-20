package se.jtutala.handlaggning.arende;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/// En **låtsad gäldenärstjänst** för tester: en riktig HTTP-server (från JDK:n) som svarar
/// som den riktiga tjänsten. Då testas ärendetjänstens HTTP-klient på riktigt – URL:er,
/// JSON, 404 och vad som händer när tjänsten är nere – utan att starta gäldenärstjänsten.
///
/// Svaren här är ett **kontrakt**: `GaldenarApiIntegrationTest` i gäldenärstjänsten
/// kontrollerar att den riktiga tjänsten svarar likadant.
final class LatsasGaldenartjanst implements AutoCloseable {

    /// Gäldenärer som finns. Alla har 10 757 kr i utmätningsbart belopp per månad.
    private static final Set<Long> FINNS = Set.of(1L, 2L, 3L, 42L);
    private static final Pattern URL = Pattern.compile("/api/galdenarer/(\\d+)(/forbehallsbelopp)?");

    private final HttpServer server;
    private volatile boolean trasig;

    LatsasGaldenartjanst() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api/galdenarer/", this::svara);
        server.start();
    }

    String url() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    /// Låtsas att tjänsten har problem: svarar 500 på allt.
    void bliTrasig(boolean trasig) {
        this.trasig = trasig;
    }

    private void svara(HttpExchange anrop) throws IOException {
        Matcher m = URL.matcher(anrop.getRequestURI().getPath());
        if (trasig) {
            skicka(anrop, 500, "{\"title\":\"Internal Server Error\"}");
        } else if (!m.matches() || !FINNS.contains(Long.parseLong(m.group(1)))) {
            skicka(anrop, 404, "{\"title\":\"Hittades inte\"}");
        } else if (m.group(2) != null) {
            skicka(anrop, 200, """
                    {"normalbeloppsar":2026,"normalbelopp":6243,"boendekostnad":8000,
                     "forbehallsbelopp":14243,"nettoinkomst":25000,"utmatningsbartBelopp":10757}""");
        } else {
            skicka(anrop, 200, "{\"id\":" + m.group(1) + ",\"namn\":\"Test Testsson\"}");
        }
    }

    private static void skicka(HttpExchange anrop, int status, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        anrop.getResponseHeaders().add("Content-Type", status >= 400 ? "application/problem+json" : "application/json");
        anrop.sendResponseHeaders(status, body.length);
        try (var ut = anrop.getResponseBody()) {
            ut.write(body);
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
