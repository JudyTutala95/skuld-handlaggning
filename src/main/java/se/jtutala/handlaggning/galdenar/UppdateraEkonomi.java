package se.jtutala.handlaggning.galdenar;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import org.jspecify.annotations.NullUnmarked;

/// Kommando: uppdatera gäldenärens ekonomi och hushåll.
///
/// `Boolean` i stället för `boolean`: Jackson 3 avvisar JSON där ett primitivt fält saknas,
/// och vi vill i stället få ett tydligt valideringsfel från `@NotNull`.
@NullUnmarked
public record UppdateraEkonomi(
        @NotNull @DecimalMin("0") BigDecimal nettoinkomstPerManad,
        @NotNull @DecimalMin("0") BigDecimal boendekostnadPerManad,
        @NotNull Boolean sammanlevande,
        @NotNull List<@NotNull Integer> barnensFodelsear) {
}
