package se.jtutala.handlaggning.arende.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import se.jtutala.handlaggning.arende.domain.Arende;
import se.jtutala.handlaggning.arende.dto.ArendeResponse;
import se.jtutala.handlaggning.arende.dto.AvskrivningRequest;
import se.jtutala.handlaggning.arende.common.Kalender;

/// Användningsfall: **åtgärder som finns för flera ärendetyper** (utredning, avskrivning).
/// Om åtgärden är tillåten för just det ärendet avgör ärendets statusflöde.
@Service
public class ArendeAtgardService {

    private final ArendeHamtare hamtare;
    private final Kalender kalender;

    ArendeAtgardService(ArendeHamtare hamtare, Kalender kalender) {
        this.hamtare = hamtare;
        this.kalender = kalender;
    }

    @Transactional
    public ArendeResponse paborjaUtredning(long id) {
        Arende arende = hamtare.hamta(id);
        arende.paborjaUtredning(kalender.idag());
        return ArendeMapper.tillResponse(hamtare.spara(arende));
    }

    @Transactional
    public ArendeResponse avskriv(long id, AvskrivningRequest request) {
        Arende arende = hamtare.hamta(id);
        arende.avskriv(request.orsak(), kalender.idag());
        return ArendeMapper.tillResponse(hamtare.spara(arende));
    }
}
