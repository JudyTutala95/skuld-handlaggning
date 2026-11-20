package se.jtutala.handlaggning.arende.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.NullUnmarked;

/// Kommando: borgenären begär verkställighet av ett utslag.
@NullUnmarked
public record VerkstallighetRequest(@NotBlank @Size(max = 100) String handlaggare) {
}
