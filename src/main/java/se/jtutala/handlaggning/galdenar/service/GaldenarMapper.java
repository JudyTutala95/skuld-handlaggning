package se.jtutala.handlaggning.galdenar.service;

import java.util.Objects;

import se.jtutala.handlaggning.galdenar.domain.Ekonomi;
import se.jtutala.handlaggning.galdenar.domain.Galdenar;
import se.jtutala.handlaggning.galdenar.domain.Personnummer;
import se.jtutala.handlaggning.galdenar.dto.GaldenarResponse;
import se.jtutala.handlaggning.galdenar.dto.RegistreraGaldenarRequest;
import se.jtutala.handlaggning.galdenar.dto.UppdateraEkonomiRequest;

/// Ansvar: översätta mellan **DTO** och **domän** – åt båda hållen.
///
/// Ligger i service-lagret: det är bara services som känner till både DTO:er och domänen.
final class GaldenarMapper {

    private GaldenarMapper() {
    }

    // ---------- Request → domän ----------

    static Galdenar tillGaldenar(RegistreraGaldenarRequest request) {
        return new Galdenar(Personnummer.parse(request.personnummer()), request.fornamn().strip(),
                request.efternamn().strip(), request.adress().strip());
    }

    static Ekonomi tillEkonomi(UppdateraEkonomiRequest request, int iAr) {
        return Ekonomi.av(request.nettoinkomstPerManad(), request.boendekostnadPerManad(),
                request.sammanlevande(), request.barnensFodelsear(), iAr);
    }

    // ---------- Domän → response ----------

    static GaldenarResponse tillResponse(Galdenar g) {
        Ekonomi e = g.getEkonomi();
        return new GaldenarResponse(
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
