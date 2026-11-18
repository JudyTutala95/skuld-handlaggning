package se.jtutala.handlaggning.galdenar.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import se.jtutala.handlaggning.common.Kalender;
import se.jtutala.handlaggning.galdenar.domain.Galdenar;
import se.jtutala.handlaggning.galdenar.dto.GaldenarResponse;
import se.jtutala.handlaggning.galdenar.dto.UppdateraEkonomiRequest;
import se.jtutala.handlaggning.regelverk.Forbehallsbelopp;
import se.jtutala.handlaggning.regelverk.Forbehallsberakning;
import se.jtutala.handlaggning.regelverk.Normalbelopp;

/// Användningsfall: **gäldenärens ekonomi** – uppdatera den och räkna ut vad som kan utmätas.
@Service
@Transactional(readOnly = true)
public class GaldenarEkonomiService {

    private final GaldenarSokService sokService;
    private final Kalender kalender;

    GaldenarEkonomiService(GaldenarSokService sokService, Kalender kalender) {
        this.sokService = sokService;
        this.kalender = kalender;
    }

    @Transactional
    public GaldenarResponse uppdatera(long galdenarId, UppdateraEkonomiRequest request) {
        Galdenar galdenar = sokService.galdenar(galdenarId);
        galdenar.bytEkonomi(GaldenarMapper.tillEkonomi(request, kalender.idag().getYear()));
        return GaldenarMapper.tillResponse(galdenar);
    }

    public Forbehallsberakning forbehallsberakning(long galdenarId) {
        var hushall = sokService.galdenar(galdenarId).getEkonomi().somHushall(kalender.idag());
        return Forbehallsbelopp.berakna(hushall, Normalbelopp.AR_2026);
    }
}
