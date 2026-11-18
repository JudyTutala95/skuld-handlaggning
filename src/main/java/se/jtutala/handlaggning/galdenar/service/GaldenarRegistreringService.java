package se.jtutala.handlaggning.galdenar.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import se.jtutala.handlaggning.common.RegelbrottException;
import se.jtutala.handlaggning.galdenar.domain.Galdenar;
import se.jtutala.handlaggning.galdenar.domain.Personnummer;
import se.jtutala.handlaggning.galdenar.dto.GaldenarResponse;
import se.jtutala.handlaggning.galdenar.dto.RegistreraGaldenarRequest;
import se.jtutala.handlaggning.galdenar.repository.GaldenarRepository;

/// Användningsfall: **registrera en ny gäldenär**.
@Service
public class GaldenarRegistreringService {

    private final GaldenarRepository repository;

    GaldenarRegistreringService(GaldenarRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public GaldenarResponse registrera(RegistreraGaldenarRequest request) {
        Galdenar galdenar = GaldenarMapper.tillGaldenar(request);
        kontrolleraAttPersonnummerArLedigt(galdenar.getPersonnummer());
        return GaldenarMapper.tillResponse(repository.save(galdenar));
    }

    private void kontrolleraAttPersonnummerArLedigt(Personnummer personnummer) {
        if (repository.existsByPersonnummer(personnummer)) {
            throw new RegelbrottException("Det finns redan en gäldenär med personnummer " + personnummer.maskerat());
        }
    }
}
