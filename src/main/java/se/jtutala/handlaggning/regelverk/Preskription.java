package se.jtutala.handlaggning.regelverk;

import java.time.LocalDate;
import java.time.Month;

import org.jspecify.annotations.Nullable;

/// Beräknar när en skuld **preskriberas**, dvs. när borgenären förlorar rätten att kräva betalt.
///
/// | Måltyp                     | Preskriptionstid                                          | Källa |
/// |----------------------------|-----------------------------------------------------------|-------|
/// | Allmänt mål                | 5 år efter utgången av det kalenderår skulden förföll     | lag (1982:188) om preskription av skattefordringar m.m., 3 § |
/// | Enskilt mål                | 10 år från förfallodagen eller senaste preskriptionsavbrott | preskriptionslagen (1981:130), 2 § |
/// | Enskilt mål, konsumentfordran | 3 år (fordran mot konsument på vara/tjänst)            | preskriptionslagen 2 § andra stycket |
///
/// Ett **preskriptionsavbrott** (t.ex. att gäldenären betalar en del av skulden eller att
/// borgenären skickar ett krav) gör att tiden börjar om. Här räknar vi en inbetalning eller
/// en delgivning som avbrott.
///
/// **Förenklingar:** för allmänna mål kan Skatteverket ansöka om förlängd preskriptionstid,
/// och för enskilda mål gäller ibland andra tider. Det hanteras inte här.
public final class Preskription {

    public static final int AR_ALLMANT_MAL = 5;
    public static final int AR_ENSKILT_MAL = 10;
    public static final int AR_KONSUMENTFORDRAN = 3;

    private Preskription() {
    }

    /// @param senasteAvbrott senaste preskriptionsavbrott, eller `null` om inget skett
    public static LocalDate preskriptionsdag(Maltyp maltyp, LocalDate forfallodag, boolean konsumentfordran,
                                             @Nullable LocalDate senasteAvbrott) {
        return switch (maltyp) {
            // Allmänt mål: räknas från årets slut, inte från dagen. Avbrott påverkar inte här.
            case ALLMANT -> LocalDate.of(forfallodag.getYear() + AR_ALLMANT_MAL, Month.DECEMBER, 31);
            case ENSKILT -> {
                LocalDate start = senasteAvbrott != null && senasteAvbrott.isAfter(forfallodag) ? senasteAvbrott : forfallodag;
                yield start.plusYears(konsumentfordran ? AR_KONSUMENTFORDRAN : AR_ENSKILT_MAL);
            }
        };
    }

    public static boolean arPreskriberad(LocalDate preskriptionsdag, LocalDate idag) {
        return idag.isAfter(preskriptionsdag);
    }
}
