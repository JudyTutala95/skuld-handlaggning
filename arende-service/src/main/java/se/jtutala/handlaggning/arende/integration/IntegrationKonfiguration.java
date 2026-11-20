package se.jtutala.handlaggning.arende.integration;

import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.web.service.registry.ImportHttpServices;

/// Registrerar `GaldenarKlient` i gruppen **galdenar**. Spring Boot läser gruppens
/// inställningar från `spring.http.serviceclient.galdenar.*`.
///
/// `@EnableResilientMethods` slår på `@Retryable` och `@ConcurrencyLimit`.
@Configuration(proxyBeanMethods = false)
@ImportHttpServices(group = "galdenar", types = GaldenarKlient.class)
@EnableResilientMethods
class IntegrationKonfiguration {
}
