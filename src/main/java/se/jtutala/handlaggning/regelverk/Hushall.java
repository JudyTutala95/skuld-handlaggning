package se.jtutala.handlaggning.regelverk;

import java.math.BigDecimal;
import java.util.List;

/// Gäldenärens ekonomi och hushåll – det som behövs för att räkna ut förbehållsbeloppet.
///
/// @param nettoinkomstPerManad lön efter skatt
/// @param boendekostnadPerManad hyra/avgift för hela hushållet
/// @param sammanlevande gift eller sambo
/// @param barnensAldrar ålder på varje barn som bor i hushållet
public record Hushall(
        BigDecimal nettoinkomstPerManad,
        BigDecimal boendekostnadPerManad,
        boolean sammanlevande,
        List<Integer> barnensAldrar) {

    public Hushall {
        if (nettoinkomstPerManad.signum() < 0 || boendekostnadPerManad.signum() < 0) {
            throw new IllegalArgumentException("belopp kan inte vara negativa");
        }
        barnensAldrar = List.copyOf(barnensAldrar);
    }
}
