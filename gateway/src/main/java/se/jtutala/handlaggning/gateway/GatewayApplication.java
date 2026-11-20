package se.jtutala.handlaggning.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/// **API Gateway** (port 8080) – den enda ingången för klienter.
///
/// Klienten behöver bara känna till en adress. Gatewayen skickar varje anrop vidare till rätt
/// tjänst utifrån sökvägen (se `Rutter`). Här är också rätt plats för sådant som gäller alla
/// tjänster: inloggning, rate limiting och loggning av anrop.
@SpringBootApplication
public class GatewayApplication {

    static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
