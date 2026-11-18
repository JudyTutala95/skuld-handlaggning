package se.jtutala.handlaggning.arende;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import org.jspecify.annotations.NullUnmarked;

/// Kommando: registrera en inbetalning på skulden.
@NullUnmarked
public record RegistreraInbetalning(
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal belopp,
        @NotNull LocalDate datum) {

    public Inbetalning inbetalning() {
        return new Inbetalning(belopp, datum);
    }
}
