package se.jtutala.handlaggning.galdenar;

import jakarta.validation.constraints.NotBlank;

import org.jspecify.annotations.NullUnmarked;

/// Fråga: sök gäldenär på personnummer.
///
/// Skickas med POST i en body – inte `GET ?personnummer=` – så att personnumret
/// inte hamnar i serverloggar och webbläsarhistorik.
@NullUnmarked
public record SokGaldenar(@NotBlank String personnummer) {
}
