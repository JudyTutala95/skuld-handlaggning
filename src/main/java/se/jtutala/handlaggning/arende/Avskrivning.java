package se.jtutala.handlaggning.arende;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.NullUnmarked;

/// Kommando: avsluta ärendet utan vidare åtgärd.
@NullUnmarked
public record Avskrivning(@NotBlank @Size(max = 300) String orsak) {
}
