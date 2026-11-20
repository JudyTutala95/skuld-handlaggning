package se.jtutala.handlaggning.arende;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import se.jtutala.handlaggning.arende.dto.ArendeResponse;
import se.jtutala.handlaggning.arende.dto.DelgivningRequest;
import se.jtutala.handlaggning.arende.dto.RegistreraArendeRequest;
import se.jtutala.handlaggning.arende.regelverk.Maltyp;
import se.jtutala.handlaggning.arende.service.ArendeRegistreringService;
import se.jtutala.handlaggning.arende.service.BetalningsforelaggandeService;

/// Kontrollerar att statusbyten verkligen hamnar på **Kafka**, med rätt nyckel och JSON.
///
/// Startar en riktig Kafka i Docker (Testcontainers). `@ServiceConnection` kopplar
/// Spring Boot till den automatiskt. Finns inte Docker hoppas testet över.
///
/// JSON-formatet här är kontraktet som `notifiering-service` läser. Ändras det måste
/// mottagarna klara det.
@SpringBootTest(properties = "spring.modulith.events.externalization.enabled=true")
@Testcontainers(disabledWithoutDocker = true)
class KafkaExternaliseringTest {

    @Container
    @ServiceConnection
    static final KafkaContainer kafka = new KafkaContainer("apache/kafka:4.0.0");

    static final LatsasGaldenartjanst galdenartjanst = starta();

    @DynamicPropertySource
    static void pekaPaLatsasGaldenartjanst(DynamicPropertyRegistry registry) {
        registry.add("spring.http.serviceclient.galdenar.base-url", galdenartjanst::url);
    }

    @Autowired
    ArendeRegistreringService registrering;
    @Autowired
    BetalningsforelaggandeService betalningsforelaggande;

    @Test
    void statusbyteSkickasTillKafkaMedArendetsIdSomNyckel() {
        ArendeResponse arende = registrering.registrera(new RegistreraArendeRequest(Arendetyp.BETALNINGSFORELAGGANDE,
                Maltyp.ENSKILT, 1L, "Exempel Finans AB", new BigDecimal("20000.00"), LocalDate.of(2026, 1, 1), false, "lisa"));

        betalningsforelaggande.delge(arende.id(), new DelgivningRequest(LocalDate.of(2026, 1, 5)));

        ConsumerRecord<String, String> meddelande = lasForstaMeddelandet();
        assertThat(meddelande.key()).isEqualTo(String.valueOf(arende.id()));
        assertThat(meddelande.value())
                .contains("\"arendeId\":" + arende.id())
                .contains("\"arendenummer\":\"" + arende.arendenummer() + "\"")
                .contains("\"galdenarId\":1,")
                .contains("\"fran\":\"REGISTRERAT\"")
                .contains("\"till\":\"DELGIVET\"");
    }

    /// Läser från ämnet som en fristående mottagare skulle göra – med text, inte Java-klasser.
    private static ConsumerRecord<String, String> lasForstaMeddelandet() {
        Map<String, Object> config = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "test",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        try (var konsument = new KafkaConsumer<>(config, new StringDeserializer(), new StringDeserializer())) {
            konsument.subscribe(List.of(ArendeStatusAndrad.AMNE));
            List<ConsumerRecord<String, String>> lasta = new ArrayList<>();
            long slut = System.currentTimeMillis() + 20_000;
            while (lasta.isEmpty() && System.currentTimeMillis() < slut) {
                konsument.poll(Duration.ofMillis(500)).forEach(lasta::add);
            }
            assertThat(lasta).as("meddelanden på %s", ArendeStatusAndrad.AMNE).isNotEmpty();
            return lasta.getFirst();
        }
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
}
