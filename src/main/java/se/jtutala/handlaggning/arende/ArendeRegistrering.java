package se.jtutala.handlaggning.arende;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import se.jtutala.handlaggning.common.Kalender;
import se.jtutala.handlaggning.galdenar.GaldenarUppslag;

/// Användningsfall: **registrera ett nytt ärende**.
@Service
public class ArendeRegistrering {

    private final Arendelager lager;
    private final GaldenarUppslag galdenarer;
    private final Kalender kalender;

    ArendeRegistrering(Arendelager lager, GaldenarUppslag galdenarer, Kalender kalender) {
        this.lager = lager;
        this.galdenarer = galdenarer;
        this.kalender = kalender;
    }

    @Transactional
    public ArendeSvar registrera(RegistreraArende kommando) {
        galdenarer.kontrolleraAttFinns(kommando.galdenarId());
        Arende arende = ArendeFabrik.skapa(kommando.typ(), kommando.maltyp(), kommando.galdenarId(),
                kommando.fordran(), kommando.handlaggare().strip(), kalender.idag());
        return ArendeSvarMapper.tillSvar(lager.spara(arende));
    }
}
