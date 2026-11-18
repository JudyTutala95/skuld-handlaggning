package se.jtutala.handlaggning.arende.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.jspecify.annotations.Nullable;

import se.jtutala.handlaggning.arende.domain.Arende;
import se.jtutala.handlaggning.arende.domain.Bestridandefrist;
import se.jtutala.handlaggning.arende.domain.Betalningsforelaggande;
import se.jtutala.handlaggning.arende.domain.Fordran;
import se.jtutala.handlaggning.arende.domain.Inbetalning;
import se.jtutala.handlaggning.arende.domain.Skuldsaneringsarende;
import se.jtutala.handlaggning.arende.domain.Skuldsaneringsperiod;
import se.jtutala.handlaggning.arende.domain.Utsokningsarende;
import se.jtutala.handlaggning.arende.dto.ArendeRadResponse;
import se.jtutala.handlaggning.arende.dto.ArendeResponse;
import se.jtutala.handlaggning.arende.dto.DagboksanteckningResponse;
import se.jtutala.handlaggning.arende.dto.InbetalningRequest;
import se.jtutala.handlaggning.arende.dto.RegistreraArendeRequest;

/// Ansvar: översätta mellan **DTO** och **domän** – åt båda hållen.
///
/// Ligger i service-lagret: varken controllers eller domänen behöver känna till den andra sidan.
final class ArendeMapper {

    private ArendeMapper() {
    }

    // ---------- Request → domän ----------

    static Fordran tillFordran(RegistreraArendeRequest request) {
        return new Fordran(request.borgenar().strip(), request.kapitalbelopp(), request.forfallodag(),
                request.arKonsumentfordran());
    }

    static Inbetalning tillInbetalning(InbetalningRequest request) {
        return new Inbetalning(request.belopp(), request.datum());
    }

    // ---------- Domän → response ----------

    static ArendeResponse tillResponse(Arende a) {
        Typdetaljer d = typdetaljer(a);
        Fordran f = a.getFordran();
        return new ArendeResponse(a.getId(), a.arendenummer(), a.typ(), a.getMaltyp(), a.getStatus(),
                a.getStatus().beskrivning(), a.getGaldenarId(), f.borgenar(), a.getHandlaggare(),
                f.kapitalbelopp(), d.inbetalt(), d.kvarstaende(), f.forfallodag(), a.getRegistrerad(),
                d.delgivningsdatum(), d.bestridandefrist(), d.loneutmatningPerManad(), d.skuldsaneringSlutar(),
                a.preskriptionsdag(), d.ursprungsarendeId(), a.mojligaNastaStatusar(), dagbok(a));
    }

    static ArendeRadResponse tillRad(Arende a) {
        return new ArendeRadResponse(a.getId(), a.arendenummer(), a.typ(), a.getStatus(), a.getGaldenarId(),
                a.getFordran().borgenar(), typdetaljer(a).kvarstaende(), a.getHandlaggare());
    }

    private static List<DagboksanteckningResponse> dagbok(Arende a) {
        return a.getDagbok().stream()
                .map(rad -> new DagboksanteckningResponse(rad.datum(), rad.handelse()))
                .toList();
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
