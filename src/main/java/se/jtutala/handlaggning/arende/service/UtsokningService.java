package se.jtutala.handlaggning.arende.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import se.jtutala.handlaggning.arende.domain.Utsokningsarende;
import se.jtutala.handlaggning.arende.dto.ArendeResponse;
import se.jtutala.handlaggning.arende.dto.InbetalningRequest;
import se.jtutala.handlaggning.common.Kalender;
import se.jtutala.handlaggning.galdenar.GaldenarApi;

/// Användningsfall för **utsökning**: löneutmätning, inbetalningar och preskription.
@Service
public class UtsokningService {

    private final ArendeHamtare hamtare;
    private final GaldenarApi galdenarApi;
    private final Kalender kalender;

    UtsokningService(ArendeHamtare hamtare, GaldenarApi galdenarApi, Kalender kalender) {
        this.hamtare = hamtare;
        this.galdenarApi = galdenarApi;
        this.kalender = kalender;
    }

    /// Beloppet räknas fram från gäldenärens ekonomi och förbehållsbeloppet.
    @Transactional
    public ArendeResponse beslutaLoneutmatning(long id) {
        Utsokningsarende arende = hamta(id);
        arende.beslutaLoneutmatning(utmatningsbartBelopp(arende), kalender.idag());
        return spara(arende);
    }

    @Transactional
    public ArendeResponse registreraInbetalning(long id, InbetalningRequest request) {
        Utsokningsarende arende = hamta(id);
        arende.registreraInbetalning(ArendeMapper.tillInbetalning(request), kalender.idag());
        return spara(arende);
    }

    @Transactional
    public ArendeResponse markeraPreskriberat(long id) {
        Utsokningsarende arende = hamta(id);
        arende.markeraPreskriberat(kalender.idag());
        return spara(arende);
    }

    private BigDecimal utmatningsbartBelopp(Utsokningsarende arende) {
        return galdenarApi.forbehallsberakning(arende.getGaldenarId()).utmatningsbartBelopp();
    }

    private Utsokningsarende hamta(long id) {
        return hamtare.hamta(id, Utsokningsarende.class);
    }

    private ArendeResponse spara(Utsokningsarende arende) {
        return ArendeMapper.tillResponse(hamtare.spara(arende));
    }
}
