package se.jtutala.handlaggning.arende;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

import org.jspecify.annotations.NullUnmarked;

/// Kommando: gäldenären har delgivits betalningsföreläggandet.
@NullUnmarked
public record Delgivning(@NotNull LocalDate datum) {
}
