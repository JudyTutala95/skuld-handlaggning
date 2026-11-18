package se.jtutala.handlaggning.galdenar;

import java.util.Objects;

/// Ansvar: översätta entiteten `Galdenar` till svaret `GaldenarSvar`.
final class GaldenarMapper {

    private GaldenarMapper() {
    }

    static GaldenarSvar tillSvar(Galdenar g) {
        Ekonomi e = g.getEkonomi();
        return new GaldenarSvar(
                Objects.requireNonNull(g.getId(), "gäldenären är inte sparad"),
                g.getPersonnummer().formaterat(),
                g.fulltNamn(),
                g.getAdress(),
                e.nettoinkomstPerManad(),
                e.boendekostnadPerManad(),
                e.sammanlevande(),
                e.barnensFodelsear());
    }
}
