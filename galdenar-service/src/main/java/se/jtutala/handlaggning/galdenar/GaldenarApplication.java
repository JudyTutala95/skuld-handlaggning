package se.jtutala.handlaggning.galdenar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/// **Gäldenärstjänsten** (port 8081).
///
/// Äger allt om gäldenärer: personuppgifter, ekonomi och förbehållsbeloppet. Har en egen
/// databas som ingen annan tjänst får läsa direkt. Andra tjänster frågar via REST-API:t:
///
/// - `GET /api/galdenarer/{id}` – finns gäldenären?
/// - `GET /api/galdenarer/{id}/forbehallsbelopp` – hur mycket av lönen kan utmätas?
@SpringBootApplication
public class GaldenarApplication {

    static void main(String[] args) {
        SpringApplication.run(GaldenarApplication.class, args);
    }
}
