package se.jtutala.handlaggning.galdenar;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.NullUnmarked;

/// DTO:er för gäldenärs-API:t – det som skickas in och ut över HTTP.
///
/// Samlade i en fil som nästlade records, så att man ser hela API-kontraktet på ett ställe.
/// `@NullUnmarked`: inkommande JSON kan sakna fält tills `@Valid` har kontrollerat dem.
@NullUnmarked
public final class GaldenarDtos {

    private GaldenarDtos() {
    }

    public record RegistreraGaldenar(
            @NotBlank String personnummer,
            @NotBlank @Size(max = 100) String fornamn,
            @NotBlank @Size(max = 100) String efternamn,
            @NotBlank @Size(max = 200) String adress) {
    }

    /// Sökning med personnummer görs med POST och body – inte `GET ?personnummer=` –
    /// så att personnumret inte hamnar i serverloggar och webbläsarhistorik.
    public record SokGaldenar(@NotBlank String personnummer) {
    }

    public record UppdateraEkonomi(
            @NotNull @DecimalMin("0") BigDecimal nettoinkomstPerManad,
            @NotNull @DecimalMin("0") BigDecimal boendekostnadPerManad,
            @NotNull Boolean sammanlevande,
            @NotNull List<@NotNull Integer> barnensFodelsear) {
    }

    public record GaldenarSvar(
            Long id,
            String personnummer,
            String namn,
            String adress,
            BigDecimal nettoinkomstPerManad,
            BigDecimal boendekostnadPerManad,
            boolean sammanlevande,
            List<Integer> barnensFodelsear) {
    }
}
