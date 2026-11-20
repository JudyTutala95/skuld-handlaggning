package se.jtutala.handlaggning.notifiering;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/// **Notifieringstjänsten** (port 8083).
///
/// Har inget REST-API för affärsfunktioner och ingen databas. Den lyssnar på Kafka-ämnet
/// `arende-status` och skickar brev till gäldenären när ett ärende byter status.
///
/// Ärendetjänsten vet inte att den här tjänsten finns. Fler tjänster kan börja lyssna på
/// samma händelser (t.ex. statistik) utan att ärendetjänsten ändras.
@SpringBootApplication
public class NotifieringApplication {

    static void main(String[] args) {
        SpringApplication.run(NotifieringApplication.class, args);
    }
}
