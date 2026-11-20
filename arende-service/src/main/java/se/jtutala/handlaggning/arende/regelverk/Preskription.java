package se.jtutala.handlaggning.arende.regelverk;

import java.time.LocalDate;
import java.time.Month;

import org.jspecify.annotations.Nullable;

/// Beräknar när en skuld **preskriberas**, dvs. när borgenären förlorar rätten att kräva betalt.
///
/// | Måltyp                        | Preskriptionstid                                            | Källa |
/// |-------------------------------|-------------------------------------------------------------|-------|
/// | Allmänt mål                   | 5 år efter utgången av det kalenderår skulden förföll       | lag (1982:188) om preskription av skattefordringar m.m., 3 § |
/// | Enskilt mål                   | 10 år från förfallodagen eller senaste preskriptionsavbrott | preskriptionslagen (1981:130), 2 § |
/// | Enskilt mål, konsumentfordran | 3 år                                                        | preskriptionslagen 2 § andra stycket |
///
/// Ett **preskriptionsavbrott** (t.ex. en delbetalning eller en delgivning) gör att tiden börjar om.
///
/// **Förenkling:** förlängd preskriptionstid för allmänna mål hanteras inte.
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
            case ALLMANT -> forAllmantMal(forfallodag);
            case ENSKILT -> forEnskiltMal(startdag(forfallodag, senasteAvbrott), konsumentfordran);
        };
    }

    public static boolean arPreskriberad(LocalDate preskriptionsdag, LocalDate idag) {
        return idag.isAfter(preskriptionsdag);
    }

    /// Räknas från årets slut. Avbrott påverkar inte.
    private static LocalDate forAllmantMal(LocalDate forfallodag) {
        return LocalDate.of(forfallodag.getYear() + AR_ALLMANT_MAL, Month.DECEMBER, 31);
    }

    private static LocalDate forEnskiltMal(LocalDate startdag, boolean konsumentfordran) {
        return startdag.plusYears(konsumentfordran ? AR_KONSUMENTFORDRAN : AR_ENSKILT_MAL);
    }

    /// Tiden räknas från förfallodagen, eller från ett senare avbrott.
    private static LocalDate startdag(LocalDate forfallodag, @Nullable LocalDate senasteAvbrott) {
        return senasteAvbrott != null && senasteAvbrott.isAfter(forfallodag) ? senasteAvbrott : forfallodag;
    }
}
