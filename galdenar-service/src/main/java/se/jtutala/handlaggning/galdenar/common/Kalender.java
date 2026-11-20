package se.jtutala.handlaggning.galdenar.common;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.stereotype.Component;

/// Svarar på en enda fråga: **vilket datum är det idag?**
///
/// Affärskoden frågar `Kalender` i stället för att anropa `LocalDate.now()` direkt.
/// Då kan tester styra datumet via `Clock` (se `KlockKonfiguration`).
@Component
public class Kalender {

    private final Clock clock;

    Kalender(Clock clock) {
        this.clock = clock;
    }

    public LocalDate idag() {
        return LocalDate.now(clock);
    }
}
