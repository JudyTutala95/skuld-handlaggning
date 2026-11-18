package se.jtutala.handlaggning.arende;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/// En rad i ärendets **dagbok**. Enligt utsökningsförordningen ska Kronofogden föra dagbok
/// i varje mål – en spårbar historik över allt som hänt. Dagboken skrivs bara till, aldrig ändras.
///
/// En record som `@Embeddable` (stöds sedan Hibernate 6.2): oföränderlig och kompakt.
@Embeddable
public record Dagboksanteckning(
        @Column(nullable = false) LocalDate datum,
        @Column(nullable = false, length = 500) String handelse) {
}
