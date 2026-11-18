package se.jtutala.handlaggning.arende;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.NullUnmarked;

/// Kommando: bevilja eller avslå en ansökan om skuldsanering.
@NullUnmarked
public record Skuldsaneringsbeslut(@NotNull Boolean beviljas, @NotBlank @Size(max = 300) String motivering) {
}
