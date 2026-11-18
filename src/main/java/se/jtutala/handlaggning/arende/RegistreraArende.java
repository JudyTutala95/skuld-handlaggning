package se.jtutala.handlaggning.arende;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.NullUnmarked;

import se.jtutala.handlaggning.regelverk.Maltyp;

/// Kommando: registrera ett nytt ärende.
///
/// `@NullUnmarked`: JSON från klienten kan sakna fält tills `@Valid` har kontrollerat dem.
@NullUnmarked
public record RegistreraArende(
        @NotNull Arendetyp typ,
        @NotNull Maltyp maltyp,
        @NotNull @Positive Long galdenarId,
        @NotBlank @Size(max = 200) String borgenar,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal kapitalbelopp,
        @NotNull LocalDate forfallodag,
        Boolean konsumentfordran,
        @NotBlank @Size(max = 100) String handlaggare) {

    /// Valfritt fält: saknas det räknas fordran inte som konsumentfordran.
    /// (`Boolean`, inte `boolean` – Jackson 3 avvisar JSON där ett primitivt fält saknas.)
    public boolean arKonsumentfordran() {
        return Boolean.TRUE.equals(konsumentfordran);
    }

    public Fordran fordran() {
        return new Fordran(borgenar.strip(), kapitalbelopp, forfallodag, arKonsumentfordran());
    }
}
