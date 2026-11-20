package se.jtutala.handlaggning.notifiering;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

/// Skickar en händelse till en riktig Kafka (Testcontainers) och kontrollerar att ett brev
/// skickas. Finns inte Docker hoppas testet över.
///
/// JSON:en är exakt det format som ärendetjänsten skickar (se `KafkaExternaliseringTest`
/// där), plus ett okänt fält – som ska ignoreras.
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class BrevUtskickKafkaTest {

    @Container
    @ServiceConnection
    static final KafkaContainer kafka = new KafkaContainer("apache/kafka:4.0.0");

    @MockitoBean
    Brevlada brevlada;

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void delgivningFranArendetjanstenGerEttBrev() {
        kafkaTemplate.send("arende-status", "7", """
                {"arendeId":7,"arendenummer":"BF-2026-000007","galdenarId":3,"typ":"BETALNINGSFORELAGGANDE",
                 "fran":"REGISTRERAT","till":"DELGIVET","nyttFaltSomViInteKanner":"ignoreras"}""");

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                verify(brevlada).skicka(eq(3L), contains("BF-2026-000007")));
    }

    @Test
    void trasigtMeddelandeStopparInteDeSomKommerEfter() {
        kafkaTemplate.send("arende-status", "8", "det här är inte JSON");
        kafkaTemplate.send("arende-status", "9", """
                {"arendeId":9,"arendenummer":"UTS-2026-000009","galdenarId":2,"typ":"UTSOKNING",
                 "fran":"UTREDNING","till":"LONEUTMATNING"}""");

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                verify(brevlada).skicka(eq(2L), contains("löneutmätning")));
    }
}
