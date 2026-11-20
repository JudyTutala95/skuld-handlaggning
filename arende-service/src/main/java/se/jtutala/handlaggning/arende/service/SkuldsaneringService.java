package se.jtutala.handlaggning.arende.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import se.jtutala.handlaggning.arende.domain.Skuldsaneringsarende;
import se.jtutala.handlaggning.arende.dto.ArendeResponse;
import se.jtutala.handlaggning.arende.dto.SkuldsaneringsbeslutRequest;
import se.jtutala.handlaggning.arende.common.Kalender;

/// Användningsfall för **skuldsanering**: beslut och slutförande.
@Service
public class SkuldsaneringService {

    private final ArendeHamtare hamtare;
    private final Kalender kalender;

    SkuldsaneringService(ArendeHamtare hamtare, Kalender kalender) {
        this.hamtare = hamtare;
        this.kalender = kalender;
    }

    @Transactional
    public ArendeResponse besluta(long id, SkuldsaneringsbeslutRequest beslut) {
        Skuldsaneringsarende arende = hamta(id);
        if (beslut.beviljas()) {
            arende.bevilja(beslut.motivering(), kalender.idag());
        } else {
            arende.avsla(beslut.motivering(), kalender.idag());
        }
        return spara(arende);
    }

    @Transactional
    public ArendeResponse slutfor(long id) {
        Skuldsaneringsarende arende = hamta(id);
        arende.slutfor(kalender.idag());
        return spara(arende);
    }

    private Skuldsaneringsarende hamta(long id) {
        return hamtare.hamta(id, Skuldsaneringsarende.class);
    }

    private ArendeResponse spara(Skuldsaneringsarende arende) {
        return ArendeMapper.tillResponse(hamtare.spara(arende));
    }
}
