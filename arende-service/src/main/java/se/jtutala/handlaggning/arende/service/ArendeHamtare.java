package se.jtutala.handlaggning.arende.service;

import org.springframework.stereotype.Component;

import se.jtutala.handlaggning.arende.domain.Arende;
import se.jtutala.handlaggning.arende.repository.ArendeRepository;
import se.jtutala.handlaggning.arende.common.HittadesInteException;
import se.jtutala.handlaggning.arende.common.RegelbrottException;

/// Hjälpklass för services: **hämta och spara ärenden** – och kontrollera att ett ärende
/// är av rätt typ för åtgärden (t.ex. att bara betalningsförelägganden kan delges).
@Component
class ArendeHamtare {

    private final ArendeRepository repository;

    ArendeHamtare(ArendeRepository repository) {
        this.repository = repository;
    }

    Arende hamta(long id) {
        return repository.findById(id).orElseThrow(() -> new HittadesInteException("Ärende", id));
    }

    <T extends Arende> T hamta(long id, Class<T> forvantadTyp) {
        Arende arende = hamta(id);
        if (!forvantadTyp.isInstance(arende)) {
            throw new RegelbrottException("Åtgärden gäller inte ärenden av typen " + arende.typ());
        }
        return forvantadTyp.cast(arende);
    }

    /// `save()` behövs även för redan sparade ärenden: det är då Spring Data publicerar
    /// händelserna som ärendet registrerat.
    <T extends Arende> T spara(T arende) {
        return repository.save(arende);
    }
}
