package se.jtutala.handlaggning.gateway;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.web.servlet.function.RequestPredicates.path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/// Vilken sökväg går till vilken tjänst. Adresserna står i `application.properties`
/// (`tjanster.*`) och byts ut i Docker Compose.
///
/// | Sökväg              | Tjänst              |
/// |---------------------|---------------------|
/// | `/api/galdenarer/**` | galdenar-service   |
/// | `/api/arenden/**`    | arende-service     |
@Configuration(proxyBeanMethods = false)
class Rutter {

    @Bean
    RouterFunction<ServerResponse> galdenarRutt(@Value("${tjanster.galdenar}") String adress) {
        return route("galdenar-service")
                .route(path("/api/galdenarer/**"), http())
                .before(uri(adress))
                .build();
    }

    @Bean
    RouterFunction<ServerResponse> arendeRutt(@Value("${tjanster.arende}") String adress) {
        return route("arende-service")
                .route(path("/api/arenden/**"), http())
                .before(uri(adress))
                .build();
    }
}
