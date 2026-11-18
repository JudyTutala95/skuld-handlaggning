package se.jtutala.handlaggning.arende;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.jspecify.annotations.NullUnmarked;

import se.jtutala.handlaggning.regelverk.Maltyp;

/// Svar: ett ärende med alla detaljer och dagbok. Fält som inte gäller ärendetypen är `null`.
@NullUnmarked
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
}
