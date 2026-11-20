package se.jtutala.handlaggning.arende.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import se.jtutala.handlaggning.arende.domain.Arende;
import se.jtutala.handlaggning.arende.domain.ArendeFabrik;
import se.jtutala.handlaggning.arende.dto.ArendeResponse;
import se.jtutala.handlaggning.arende.dto.RegistreraArendeRequest;
import se.jtutala.handlaggning.arende.common.Kalender;
import se.jtutala.handlaggning.arende.integration.Galdenarregister;

/// Användningsfall: **registrera ett nytt ärende**.
@Service
public class ArendeRegistreringService {

    private final ArendeHamtare hamtare;
    private final Galdenarregister galdenarregister;
    private final Kalender kalender;

    ArendeRegistreringService(ArendeHamtare hamtare, Galdenarregister galdenarregister, Kalender kalender) {
        this.hamtare = hamtare;
        this.galdenarregister = galdenarregister;
        this.kalender = kalender;
    }

    @Transactional
    public ArendeResponse registrera(RegistreraArendeRequest request) {
        galdenarregister.kontrolleraAttFinns(request.galdenarId());
        Arende arende = ArendeFabrik.skapa(request.typ(), request.maltyp(), request.galdenarId(),
                ArendeMapper.tillFordran(request), request.handlaggare().strip(), kalender.idag());
        return ArendeMapper.tillResponse(hamtare.spara(arende));
    }
}
