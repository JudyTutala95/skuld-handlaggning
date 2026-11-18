package se.jtutala.handlaggning.galdenar;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import se.jtutala.handlaggning.common.RegelbrottException;

/// Användningsfall: **registrera en ny gäldenär**.
@Service
public class GaldenarRegistrering {

    private final GaldenarRepository repository;

    GaldenarRegistrering(GaldenarRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public GaldenarSvar registrera(RegistreraGaldenar kommando) {
        Personnummer personnummer = Personnummer.parse(kommando.personnummer());
        kontrolleraAttPersonnummerArLedigt(personnummer);
        Galdenar sparad = repository.save(nyGaldenar(personnummer, kommando));
        return GaldenarMapper.tillSvar(sparad);
    }

    private void kontrolleraAttPersonnummerArLedigt(Personnummer personnummer) {
        if (repository.existsByPersonnummer(personnummer)) {
            throw new RegelbrottException("Det finns redan en gäldenär med personnummer " + personnummer.maskerat());
        }
    }

    private static Galdenar nyGaldenar(Personnummer personnummer, RegistreraGaldenar kommando) {
        return new Galdenar(personnummer, kommando.fornamn().strip(), kommando.efternamn().strip(), kommando.adress().strip());
    }
}
