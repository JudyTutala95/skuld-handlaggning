package se.jtutala.handlaggning.galdenar.common;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/// Systemets klocka som en bean.
///
/// Handläggning handlar mycket om datum (frister, preskription, skuldsanering i 5 år).
/// Använd ALDRIG `LocalDate.now()` direkt i affärskod – injicera `Clock` i stället.
/// Då kan tester styra "idag" och testa t.ex. "dagen efter att fristen gått ut".

@Configuration(proxyBeanMethods = false)
public class KlockKonfiguration {

    public static final ZoneId SVERIGE = ZoneId.of("Europe/Stockholm");

    @Bean
    Clock clock() {
        return Clock.system(SVERIGE);
    }
}
