package se.jtutala.handlaggning.arende;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import se.jtutala.handlaggning.common.Kalender;

/// Användningsfall: **åtgärder som finns för flera ärendetyper** (utredning, avskrivning).
/// Om åtgärden är tillåten för just det ärendet avgör ärendets statusflöde.
@Service
public class GemensammaAtgarder {

    private final Arendelager lager;
    private final Kalender kalender;

    GemensammaAtgarder(Arendelager lager, Kalender kalender) {
        this.lager = lager;
        this.kalender = kalender;
    }

    @Transactional
    public ArendeSvar paborjaUtredning(long id) {
        Arende arende = lager.hamta(id);
        arende.paborjaUtredning(kalender.idag());
        return ArendeSvarMapper.tillSvar(lager.spara(arende));
    }

    @Transactional
    public ArendeSvar avskriv(long id, String orsak) {
        Arende arende = lager.hamta(id);
        arende.avskriv(orsak, kalender.idag());
        return ArendeSvarMapper.tillSvar(lager.spara(arende));
    }
}
