package se.jtutala.handlaggning.arende;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/// En inbetalning på skulden (frivillig betalning eller från löneutmätning).
@Embeddable
public record Inbetalning(
        @Column(nullable = false, precision = 12, scale = 2) BigDecimal belopp,
        @Column(nullable = false) LocalDate datum) {
}
