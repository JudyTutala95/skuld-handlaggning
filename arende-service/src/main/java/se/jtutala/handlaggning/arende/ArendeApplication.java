package se.jtutala.handlaggning.arende;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/// **Ärendetjänsten** (port 8082).
///
/// Äger alla ärenden: betalningsföreläggande, utsökning och skuldsanering.
///
/// - Frågar **gäldenärstjänsten** via REST (synkront) om gäldenären finns och hur mycket
///   av lönen som kan utmätas. Se `integration.Galdenarregister`.
/// - Skickar händelsen `ArendeStatusAndrad` till **Kafka** (asynkront) när ett ärende byter
///   status. Ärendetjänsten vet inte vem som lyssnar.
@SpringBootApplication
public class ArendeApplication {

    static void main(String[] args) {
        SpringApplication.run(ArendeApplication.class, args);
    }
}
