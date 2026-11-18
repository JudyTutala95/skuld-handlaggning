package se.jtutala.handlaggning.notifiering;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/// Aktiverar `@Async`, som `@ApplicationModuleListener` bygger på.
/// Med virtuella trådar påslagna kör Spring Boot asynkrona uppgifter på virtuella trådar.
@Configuration(proxyBeanMethods = false)
@EnableAsync
class AsyncKonfiguration {
}
