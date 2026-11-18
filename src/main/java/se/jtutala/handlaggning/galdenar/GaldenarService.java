package se.jtutala.handlaggning.galdenar;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import se.jtutala.handlaggning.common.HittadesInteException;
import se.jtutala.handlaggning.common.RegelbrottException;
import se.jtutala.handlaggning.galdenar.GaldenarDtos.GaldenarSvar;
import se.jtutala.handlaggning.galdenar.GaldenarDtos.RegistreraGaldenar;
import se.jtutala.handlaggning.galdenar.GaldenarDtos.UppdateraEkonomi;
import se.jtutala.handlaggning.regelverk.Forbehallsbelopp;
import se.jtutala.handlaggning.regelverk.Forbehallsberakning;
import se.jtutala.handlaggning.regelverk.Hushall;
import se.jtutala.handlaggning.regelverk.Normalbelopp;

/// Gäldenärsmodulens **publika API**. Andra moduler (t.ex. `arende`) använder bara den här
/// klassen – aldrig `Galdenar` eller `GaldenarRepository` direkt.
@Service
@Transactional(readOnly = true)
public class GaldenarService {

    private final GaldenarRepository repository;
    private final Clock clock;

    GaldenarService(GaldenarRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public GaldenarSvar registrera(RegistreraGaldenar kommando) {
        Personnummer pnr = Personnummer.parse(kommando.personnummer());
        if (repository.existsByPersonnummer(pnr)) {
            throw new RegelbrottException("Det finns redan en gäldenär med personnummer " + pnr.maskerat());
        }
        Galdenar sparad = repository.save(new Galdenar(pnr, kommando.fornamn().strip(),
                kommando.efternamn().strip(), kommando.adress().strip()));
        return svar(sparad);
    }

    public GaldenarSvar hamta(long id) {
        return svar(galdenar(id));
    }

    public GaldenarSvar sok(String personnummer) {
        Personnummer pnr = Personnummer.parse(personnummer);
        return repository.findByPersonnummer(pnr).map(GaldenarService::svar)
                .orElseThrow(() -> new HittadesInteException("Gäldenär", pnr.maskerat()));
    }

    @Transactional
    public GaldenarSvar uppdateraEkonomi(long id, UppdateraEkonomi kommando) {
        int iAr = LocalDate.now(clock).getYear();
        if (kommando.barnensFodelsear().stream().anyMatch(ar -> ar > iAr || ar < iAr - 25)) {
            throw new IllegalArgumentException("orimligt födelseår för barn");
        }
        Galdenar g = galdenar(id);
        g.uppdateraEkonomi(kommando.nettoinkomstPerManad(), kommando.boendekostnadPerManad(),
                kommando.sammanlevande(), kommando.barnensFodelsear());
        return svar(g);
    }

    /// Gäldenärens hushåll – används av ärendemodulen vid beslut om löneutmätning.
    public Hushall hushall(long id) {
        return galdenar(id).hushall(LocalDate.now(clock));
    }

    public Forbehallsberakning beraknaForbehallsbelopp(long id) {
        return Forbehallsbelopp.berakna(hushall(id), Normalbelopp.AR_2026);
    }

    public void kontrolleraAttFinns(long id) {
        if (!repository.existsById(id)) {
            throw new HittadesInteException("Gäldenär", id);
        }
    }

    private Galdenar galdenar(long id) {
        return repository.findById(id).orElseThrow(() -> new HittadesInteException("Gäldenär", id));
    }

    private static GaldenarSvar svar(Galdenar g) {
        return new GaldenarSvar(Objects.requireNonNull(g.getId()), g.getPersonnummer().formaterat(),
                g.getFornamn() + " " + g.getEfternamn(), g.getAdress(),
                g.getNettoinkomstPerManad(), g.getBoendekostnadPerManad(), g.isSammanlevande(), g.getBarnensFodelsear());
    }
}
