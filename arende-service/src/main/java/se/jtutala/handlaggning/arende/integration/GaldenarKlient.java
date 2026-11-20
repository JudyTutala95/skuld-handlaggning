package se.jtutala.handlaggning.arende.integration;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/// **HTTP Service Client** för gäldenärstjänstens REST-API (Spring Framework 7).
///
/// Bara ett interface – Spring skapar implementationen som gör HTTP-anropen.
/// Adressen och timeouts står i `application.properties` under
/// `spring.http.serviceclient.galdenar.*`.
@HttpExchange(url = "/api/galdenarer", accept = "application/json")
interface GaldenarKlient {

    /// Kastar `HttpClientErrorException.NotFound` om gäldenären inte finns.
    @GetExchange("/{id}")
    void hamta(@PathVariable long id);

    @GetExchange("/{id}/forbehallsbelopp")
    ForbehallsberakningSvar forbehallsbelopp(@PathVariable long id);
}
