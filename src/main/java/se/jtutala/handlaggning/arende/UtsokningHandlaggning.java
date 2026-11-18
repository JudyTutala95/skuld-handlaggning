package se.jtutala.handlaggning.arende;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import se.jtutala.handlaggning.common.Kalender;
import se.jtutala.handlaggning.galdenar.GaldenarEkonomi;

/// Användningsfall för **utsökning**: löneutmätning, inbetalningar och preskription.
@Service
public class UtsokningHandlaggning {

    private final Arendelager lager;
    private final GaldenarEkonomi galdenarEkonomi;
    private final Kalender kalender;

    UtsokningHandlaggning(Arendelager lager, GaldenarEkonomi galdenarEkonomi, Kalender kalender) {
        this.lager = lager;
        this.galdenarEkonomi = galdenarEkonomi;
        this.kalender = kalender;
    }

    /// Beloppet räknas fram från gäldenärens ekonomi och förbehållsbeloppet.
    @Transactional
    public ArendeSvar beslutaLoneutmatning(long id) {
        Utsokningsarende arende = hamta(id);
        arende.beslutaLoneutmatning(utmatningsbartBelopp(arende), kalender.idag());
        return spara(arende);
    }

    @Transactional
    public ArendeSvar registreraInbetalning(long id, Inbetalning inbetalning) {
        Utsokningsarende arende = hamta(id);
        arende.registreraInbetalning(inbetalning, kalender.idag());
        return spara(arende);
    }

    @Transactional
    public ArendeSvar markeraPreskriberat(long id) {
        Utsokningsarende arende = hamta(id);
        arende.markeraPreskriberat(kalender.idag());
        return spara(arende);
    }

    private BigDecimal utmatningsbartBelopp(Utsokningsarende arende) {
        return galdenarEkonomi.forbehallsberakning(arende.getGaldenarId()).utmatningsbartBelopp();
    }

    private Utsokningsarende hamta(long id) {
        return lager.hamta(id, Utsokningsarende.class);
    }

    private ArendeSvar spara(Utsokningsarende arende) {
        return ArendeSvarMapper.tillSvar(lager.spara(arende));
    }
}
