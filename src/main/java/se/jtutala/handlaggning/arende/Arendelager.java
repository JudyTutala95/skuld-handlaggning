package se.jtutala.handlaggning.arende;

import org.springframework.stereotype.Component;

import se.jtutala.handlaggning.common.HittadesInteException;
import se.jtutala.handlaggning.common.RegelbrottException;

/// Ansvar: **hämta och spara ärenden** – och kontrollera att ett ärende är av rätt typ
/// för den åtgärd som ska göras (t.ex. att bara betalningsförelägganden kan delges).
@Component
class Arendelager {

    private final ArendeRepository repository;

    Arendelager(ArendeRepository repository) {
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
