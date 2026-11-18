package se.jtutala.handlaggning.arende;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

/// Ansvar: översätta ett ärende till API-svar (`ArendeSvar`, `ArendeRad`).
///
/// Fält som bara finns för vissa ärendetyper samlas i `Typdetaljer`, som fylls i med
/// pattern matching på ärendets klass.
final class ArendeSvarMapper {

    private ArendeSvarMapper() {
    }

    static ArendeSvar tillSvar(Arende a) {
        Typdetaljer d = typdetaljer(a);
        Fordran f = a.getFordran();
        return new ArendeSvar(a.getId(), a.arendenummer(), a.typ(), a.getMaltyp(), a.getStatus(),
                a.getStatus().beskrivning(), a.getGaldenarId(), f.borgenar(), a.getHandlaggare(),
                f.kapitalbelopp(), d.inbetalt(), d.kvarstaende(), f.forfallodag(), a.getRegistrerad(),
                d.delgivningsdatum(), d.bestridandefrist(), d.loneutmatningPerManad(), d.skuldsaneringSlutar(),
                a.preskriptionsdag(), d.ursprungsarendeId(), a.mojligaNastaStatusar(), a.getDagbok());
    }

    static ArendeRad tillRad(Arende a) {
        return new ArendeRad(a.getId(), a.arendenummer(), a.typ(), a.getStatus(), a.getGaldenarId(),
                a.getFordran().borgenar(), typdetaljer(a).kvarstaende(), a.getHandlaggare());
    }

    /// Uppgifter som skiljer sig mellan ärendetyperna.
    private record Typdetaljer(
            BigDecimal inbetalt,
            BigDecimal kvarstaende,
            @Nullable LocalDate delgivningsdatum,
            @Nullable LocalDate bestridandefrist,
            @Nullable BigDecimal loneutmatningPerManad,
            @Nullable LocalDate skuldsaneringSlutar,
            @Nullable Long ursprungsarendeId) {
    }

    private static Typdetaljer typdetaljer(Arende a) {
        return switch (a) {
            case Betalningsforelaggande bf -> forBetalningsforelaggande(bf);
            case Utsokningsarende uts -> forUtsokning(uts);
            case Skuldsaneringsarende sks -> forSkuldsanering(sks);
            default -> throw new IllegalStateException("Okänd ärendeklass: " + a.getClass());
        };
    }

    private static Typdetaljer forBetalningsforelaggande(Betalningsforelaggande bf) {
        Bestridandefrist frist = bf.bestridandefrist();
        return new Typdetaljer(BigDecimal.ZERO, bf.getFordran().kapitalbelopp(), bf.getDelgivningsdatum(),
                frist == null ? null : frist.sistaDag(), null, null, null);
    }

    private static Typdetaljer forUtsokning(Utsokningsarende uts) {
        return new Typdetaljer(uts.inbetaltTotalt(), uts.kvarstaendeSkuld(), null, null,
                uts.getLoneutmatningPerManad(), null, uts.getUrsprungsarendeId());
    }

    private static Typdetaljer forSkuldsanering(Skuldsaneringsarende sks) {
        Skuldsaneringsperiod period = sks.period();
        return new Typdetaljer(BigDecimal.ZERO, sks.getFordran().kapitalbelopp(), null, null, null,
                period == null ? null : period.slutar(), null);
    }
}
