package se.jtutala.handlaggning.arende;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import se.jtutala.handlaggning.common.RegelbrottException;

/// En inbetalning på skulden (frivillig betalning eller från löneutmätning).
@Embeddable
public record Inbetalning(
        @Column(nullable = false, precision = 12, scale = 2) BigDecimal belopp,
        @Column(nullable = false) LocalDate datum) {

    public Inbetalning {
        if (belopp.signum() <= 0) {
            throw new RegelbrottException("Inbetalningen måste vara större än 0 kr");
        }
    }
}
