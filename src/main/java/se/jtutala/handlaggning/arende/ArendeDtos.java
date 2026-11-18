package se.jtutala.handlaggning.arende;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.NullUnmarked;

import se.jtutala.handlaggning.regelverk.Maltyp;

/// DTO:er för ärende-API:t. **Kommandon** (det handläggaren vill göra) och **svar**.
///
/// Varje åtgärd har ett eget, litet kommando i stället för en stor "uppdatera ärende"-DTO.
/// Det gör API:t självförklarande: `POST /api/arenden/7/bestridande` med `{datum, grund}`.
@NullUnmarked
public final class ArendeDtos {

    private ArendeDtos() {
    }

    // ---------- Kommandon ----------

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
        /// (`Boolean` i stället för `boolean` – Jackson 3 avvisar annars JSON där ett
        /// primitivt fält saknas.)
        public boolean arKonsumentfordran() {
            return Boolean.TRUE.equals(konsumentfordran);
        }
    }

    public record Delgivning(@NotNull LocalDate datum) {
    }

    public record Bestridande(@NotNull LocalDate datum, @NotBlank @Size(max = 300) String grund) {
    }

    public record Verkstallighet(@NotBlank @Size(max = 100) String handlaggare) {
    }

    public record RegistreraInbetalning(
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal belopp,
            @NotNull LocalDate datum) {
    }

    public record Skuldsaneringsbeslut(@NotNull Boolean beviljas, @NotBlank @Size(max = 300) String motivering) {
    }

    public record Avskrivning(@NotBlank @Size(max = 300) String orsak) {
    }

    // ---------- Svar ----------

    public record ArendeSvar(
            Long id,
            String arendenummer,
            Arendetyp typ,
            Maltyp maltyp,
            Arendestatus status,
            String statusbeskrivning,
            Long galdenarId,
            String borgenar,
            String handlaggare,
            BigDecimal kapitalbelopp,
            BigDecimal inbetalt,
            BigDecimal kvarstaendeSkuld,
            LocalDate forfallodag,
            LocalDate registrerad,
            LocalDate delgivningsdatum,
            LocalDate bestridandefrist,
            BigDecimal loneutmatningPerManad,
            LocalDate skuldsaneringSlutar,
            LocalDate preskriptionsdag,
            Long ursprungsarendeId,
            List<Arendestatus> mojligaNastaStatusar,
            List<Dagboksanteckning> dagbok) {

        static ArendeSvar fran(Arende a) {
            return new ArendeSvar(a.getId(), a.arendenummer(), a.getTyp(), a.getMaltyp(), a.getStatus(),
                    a.getStatus().beskrivning(), a.getGaldenarId(), a.getBorgenar(), a.getHandlaggare(),
                    a.getKapitalbelopp(), a.inbetaltTotalt(), a.kvarstaendeSkuld(), a.getForfallodag(),
                    a.getRegistrerad(), a.getDelgivningsdatum(), a.bestridandefrist(), a.getLoneutmatningPerManad(),
                    a.skuldsaneringSlutar(), a.preskriptionsdag(), a.getUrsprungsarendeId(),
                    a.mojligaNastaStatusar(), a.getDagbok());
        }
    }

    /// Kortare form för listor (utan dagbok).
    public record ArendeRad(Long id, String arendenummer, Arendetyp typ, Arendestatus status,
                            Long galdenarId, String borgenar, BigDecimal kvarstaendeSkuld, String handlaggare) {

        static ArendeRad fran(Arende a) {
            return new ArendeRad(a.getId(), a.arendenummer(), a.getTyp(), a.getStatus(), a.getGaldenarId(),
                    a.getBorgenar(), a.kvarstaendeSkuld(), a.getHandlaggare());
        }
    }
}
