package se.jtutala.handlaggning.arende;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.NullUnmarked;

/// Kommando: gäldenären bestrider kravet.
@NullUnmarked
public record Bestridande(@NotNull LocalDate datum, @NotBlank @Size(max = 300) String grund) {
}
