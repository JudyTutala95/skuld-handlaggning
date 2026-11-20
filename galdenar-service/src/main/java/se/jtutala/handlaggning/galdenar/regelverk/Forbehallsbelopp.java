package se.jtutala.handlaggning.galdenar.regelverk;

import java.math.BigDecimal;
import java.math.RoundingMode;

/// Beräknar **förbehållsbeloppet** vid löneutmätning: den del av lönen som gäldenären
/// alltid får behålla för att kunna leva. Resten av nettolönen kan utmätas.
///
/// ```
/// förbehållsbelopp      = gäldenärens andel av (normalbelopp + boendekostnad)
/// utmätningsbart belopp = nettolön − förbehållsbelopp   (aldrig under 0, hela kronor)
/// ```
///
/// Rättslig grund: 7 kap. 5 § utsökningsbalken + Kronofogdens föreskrifter (normalbelopp).
///
/// **Förenkling:** för sammanlevande delas hushållets kostnader lika mellan de två vuxna.
/// I verkligheten fördelas de normalt efter inkomst.
///
/// Varje steg i uträkningen är en egen liten metod – läs `berakna` uppifrån och ned.
public final class Forbehallsbelopp {

    private static final BigDecimal TVA = BigDecimal.valueOf(2);

    private Forbehallsbelopp() {
    }

    public static Forbehallsberakning berakna(Hushall hushall, Normalbelopp normalbelopp) {
        BigDecimal normal = galdenarensAndel(hushall, hushalletsNormalbelopp(hushall, normalbelopp));
        BigDecimal boende = galdenarensAndel(hushall, hushall.boendekostnadPerManad());
        BigDecimal forbehall = normal.add(boende);

        return new Forbehallsberakning(normalbelopp.ar(), normal, boende, forbehall,
                hushall.nettoinkomstPerManad(), utmatningsbart(hushall.nettoinkomstPerManad(), forbehall));
    }

    /// Vuxnas normalbelopp plus ett normalbelopp per barn.
    static BigDecimal hushalletsNormalbelopp(Hushall hushall, Normalbelopp normalbelopp) {
        return vuxnasNormalbelopp(hushall, normalbelopp).add(barnensNormalbelopp(hushall, normalbelopp));
    }

    private static BigDecimal vuxnasNormalbelopp(Hushall hushall, Normalbelopp normalbelopp) {
        return hushall.sammanlevande() ? normalbelopp.sammanlevande() : normalbelopp.ensamstaende();
    }

    private static BigDecimal barnensNormalbelopp(Hushall hushall, Normalbelopp normalbelopp) {
        return hushall.barnensAldrar().stream()
                .map(normalbelopp::forBarn)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /// Sammanlevande delar på kostnaden, ensamstående bär hela.
    private static BigDecimal galdenarensAndel(Hushall hushall, BigDecimal hushalletsKostnad) {
        return hushall.sammanlevande() ? hushalletsKostnad.divide(TVA, 2, RoundingMode.HALF_UP) : hushalletsKostnad;
    }

    /// Avrundas nedåt till hela kronor – till gäldenärens fördel.
    private static BigDecimal utmatningsbart(BigDecimal nettoinkomst, BigDecimal forbehall) {
        return nettoinkomst.subtract(forbehall).max(BigDecimal.ZERO).setScale(0, RoundingMode.DOWN);
    }
}
