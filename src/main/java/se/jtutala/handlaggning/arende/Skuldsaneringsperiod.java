package se.jtutala.handlaggning.arende;

import java.time.LocalDate;

/// Perioden en beviljad skuldsanering pågår: fem år från beslutet.
public record Skuldsaneringsperiod(LocalDate beviljad) {

    public static final int AR = 5;

    public LocalDate slutar() {
        return beviljad.plusYears(AR);
    }

    public boolean arAvslutad(LocalDate idag) {
        return !idag.isBefore(slutar());
    }
}
