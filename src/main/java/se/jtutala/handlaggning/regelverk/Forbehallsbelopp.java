package se.jtutala.handlaggning.regelverk;

import java.math.BigDecimal;
import java.math.RoundingMode;

/// Beräknar **förbehållsbeloppet** vid löneutmätning: den del av lönen som gäldenären
/// alltid får behålla för att kunna leva. Resten av nettolönen kan utmätas
/// (dras från lönen och betalas till borgenärerna).
///
/// ```
/// förbehållsbelopp      = normalbelopp (vuxna + barn) + boendekostnad
/// utmätningsbart belopp = nettolön − förbehållsbelopp   (aldrig under 0)
/// ```
///
/// Rättslig grund: 7 kap. 5 § utsökningsbalken + Kronofogdens föreskrifter (normalbelopp).
///
/// **Förenkling i detta system:** för sammanlevande delas hushållets gemensamma kostnader
/// (normalbelopp för vuxna och barn samt boendet) lika mellan de två vuxna. I verkligheten
/// fördelas de normalt efter inkomst och fler kostnader kan läggas till (t.ex. resor till arbetet).
///
/// Klassen är **ren Java** – inget Spring, ingen databas. Den är lätt att testa och att läsa.
public final class Forbehallsbelopp {

    private static final BigDecimal TVA = BigDecimal.valueOf(2);

    private Forbehallsbelopp() {
    }

    public static Forbehallsberakning berakna(Hushall hushall, Normalbelopp normalbelopp) {
        BigDecimal vuxna = hushall.sammanlevande() ? normalbelopp.sammanlevande() : normalbelopp.ensamstaende();
        BigDecimal barn = hushall.barnensAldrar().stream()
                .map(normalbelopp::forBarn)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal hushalletsNormalbelopp = vuxna.add(barn);
        BigDecimal hushalletsBoende = hushall.boendekostnadPerManad();

        // Gäldenärens andel av hushållets kostnader
        BigDecimal andelNormalbelopp = hushall.sammanlevande() ? halva(hushalletsNormalbelopp) : hushalletsNormalbelopp;
        BigDecimal andelBoende = hushall.sammanlevande() ? halva(hushalletsBoende) : hushalletsBoende;

        BigDecimal forbehall = andelNormalbelopp.add(andelBoende);
        BigDecimal utmatningsbart = hushall.nettoinkomstPerManad().subtract(forbehall)
                .max(BigDecimal.ZERO)
                .setScale(0, RoundingMode.DOWN); // hela kronor, avrundat till gäldenärens fördel

        return new Forbehallsberakning(normalbelopp.ar(), andelNormalbelopp, andelBoende, forbehall,
                hushall.nettoinkomstPerManad(), utmatningsbart);
    }

    private static BigDecimal halva(BigDecimal belopp) {
        return belopp.divide(TVA, 2, RoundingMode.HALF_UP);
    }
}
