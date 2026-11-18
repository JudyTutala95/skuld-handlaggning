package se.jtutala.handlaggning.arende;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import se.jtutala.handlaggning.common.Kalender;

/// Användningsfall för **skuldsanering**: beslut och slutförande.
@Service
public class SkuldsaneringHandlaggning {

    private final Arendelager lager;
    private final Kalender kalender;

    SkuldsaneringHandlaggning(Arendelager lager, Kalender kalender) {
        this.lager = lager;
        this.kalender = kalender;
    }

    @Transactional
    public ArendeSvar bevilja(long id, String motivering) {
        Skuldsaneringsarende arende = hamta(id);
        arende.bevilja(motivering, kalender.idag());
        return spara(arende);
    }

    @Transactional
    public ArendeSvar avsla(long id, String motivering) {
        Skuldsaneringsarende arende = hamta(id);
        arende.avsla(motivering, kalender.idag());
        return spara(arende);
    }

    @Transactional
    public ArendeSvar slutfor(long id) {
        Skuldsaneringsarende arende = hamta(id);
        arende.slutfor(kalender.idag());
        return spara(arende);
    }

    private Skuldsaneringsarende hamta(long id) {
        return lager.hamta(id, Skuldsaneringsarende.class);
    }

    private ArendeSvar spara(Skuldsaneringsarende arende) {
        return ArendeSvarMapper.tillSvar(lager.spara(arende));
    }
}
