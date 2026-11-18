package se.jtutala.handlaggning.galdenar;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import se.jtutala.handlaggning.common.Kalender;
import se.jtutala.handlaggning.regelverk.Forbehallsbelopp;
import se.jtutala.handlaggning.regelverk.Forbehallsberakning;
import se.jtutala.handlaggning.regelverk.Normalbelopp;

/// Användningsfall: **gäldenärens ekonomi** – uppdatera den och räkna ut vad som kan utmätas.
@Service
@Transactional(readOnly = true)
public class GaldenarEkonomi {

    private final GaldenarUppslag uppslag;
    private final Kalender kalender;

    GaldenarEkonomi(GaldenarUppslag uppslag, Kalender kalender) {
        this.uppslag = uppslag;
        this.kalender = kalender;
    }

    @Transactional
    public GaldenarSvar uppdatera(long galdenarId, UppdateraEkonomi kommando) {
        Galdenar galdenar = uppslag.galdenar(galdenarId);
        galdenar.bytEkonomi(Ekonomi.av(kommando.nettoinkomstPerManad(), kommando.boendekostnadPerManad(),
                kommando.sammanlevande(), kommando.barnensFodelsear(), kalender.idag().getYear()));
        return GaldenarMapper.tillSvar(galdenar);
    }

    /// Hur mycket av gäldenärens lön som kan utmätas – med hela uträkningen.
    public Forbehallsberakning forbehallsberakning(long galdenarId) {
        Ekonomi ekonomi = uppslag.galdenar(galdenarId).getEkonomi();
        return Forbehallsbelopp.berakna(ekonomi.somHushall(kalender.idag()), Normalbelopp.AR_2026);
    }
}
