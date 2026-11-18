package se.jtutala.handlaggning.arende.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

import org.jspecify.annotations.NullUnmarked;

/// Kommando: gäldenären har delgivits betalningsföreläggandet.
@NullUnmarked
public record DelgivningRequest(@NotNull LocalDate datum) {
}
