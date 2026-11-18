package se.jtutala.handlaggning.galdenar;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.NullUnmarked;

/// Kommando: registrera en ny gäldenär.
///
/// `@NullUnmarked`: JSON från klienten kan sakna fält tills `@Valid` har kontrollerat dem.
@NullUnmarked
public record RegistreraGaldenar(
        @NotBlank String personnummer,
        @NotBlank @Size(max = 100) String fornamn,
        @NotBlank @Size(max = 100) String efternamn,
        @NotBlank @Size(max = 200) String adress) {
}
