package se.jtutala.handlaggning.galdenar.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import se.jtutala.handlaggning.common.HittadesInteException;
import se.jtutala.handlaggning.galdenar.domain.Galdenar;
import se.jtutala.handlaggning.galdenar.domain.Personnummer;
import se.jtutala.handlaggning.galdenar.dto.GaldenarResponse;
import se.jtutala.handlaggning.galdenar.dto.SokGaldenarRequest;
import se.jtutala.handlaggning.galdenar.repository.GaldenarRepository;

/// Användningsfall: **hitta gäldenärer** (bara läsning).
@Service
@Transactional(readOnly = true)
public class GaldenarSokService {

    private final GaldenarRepository repository;

    GaldenarSokService(GaldenarRepository repository) {
        this.repository = repository;
    }

    public GaldenarResponse hamta(long id) {
        return GaldenarMapper.tillResponse(galdenar(id));
    }

    public GaldenarResponse sok(SokGaldenarRequest request) {
        Personnummer personnummer = Personnummer.parse(request.personnummer());
        return repository.findByPersonnummer(personnummer)
                .map(GaldenarMapper::tillResponse)
                .orElseThrow(() -> new HittadesInteException("Gäldenär", personnummer.maskerat()));
    }

    void kontrolleraAttFinns(long id) {
        if (!repository.existsById(id)) {
            throw new HittadesInteException("Gäldenär", id);
        }
    }

    /// Delas med andra services i samma paket.
    Galdenar galdenar(long id) {
        return repository.findById(id).orElseThrow(() -> new HittadesInteException("Gäldenär", id));
    }
}
