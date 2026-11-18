package se.jtutala.handlaggning.galdenar;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import se.jtutala.handlaggning.common.HittadesInteException;

/// Användningsfall: **hitta gäldenärer** (bara läsning).
@Service
@Transactional(readOnly = true)
public class GaldenarUppslag {

    private final GaldenarRepository repository;

    GaldenarUppslag(GaldenarRepository repository) {
        this.repository = repository;
    }

    public GaldenarSvar hamta(long id) {
        return GaldenarMapper.tillSvar(galdenar(id));
    }

    public GaldenarSvar sokPaPersonnummer(String text) {
        Personnummer personnummer = Personnummer.parse(text);
        return repository.findByPersonnummer(personnummer)
                .map(GaldenarMapper::tillSvar)
                .orElseThrow(() -> new HittadesInteException("Gäldenär", personnummer.maskerat()));
    }

    /// Används av ärendemodulen innan ett ärende registreras.
    public void kontrolleraAttFinns(long id) {
        if (!repository.existsById(id)) {
            throw new HittadesInteException("Gäldenär", id);
        }
    }

    /// Delas med `GaldenarEkonomi` i samma modul.
    Galdenar galdenar(long id) {
        return repository.findById(id).orElseThrow(() -> new HittadesInteException("Gäldenär", id));
    }
}
