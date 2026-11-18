package se.jtutala.handlaggning.arende.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import se.jtutala.handlaggning.arende.domain.Arende;
import se.jtutala.handlaggning.arende.domain.Betalningsforelaggande;
import se.jtutala.handlaggning.arende.domain.Utsokningsarende;
import se.jtutala.handlaggning.arende.dto.ArendeResponse;
import se.jtutala.handlaggning.arende.dto.BestridandeRequest;
import se.jtutala.handlaggning.arende.dto.DelgivningRequest;
import se.jtutala.handlaggning.arende.dto.VerkstallighetRequest;
import se.jtutala.handlaggning.common.Kalender;

/// Användningsfall för **betalningsförelägganden**: delge, bestrida, utslag, tingsrätt, verkställighet.
///
/// Varje metod följer samma mönster: hämta → låt domänobjektet göra jobbet → spara → svara.
@Service
public class BetalningsforelaggandeService {

    private final ArendeHamtare hamtare;
    private final Kalender kalender;

    BetalningsforelaggandeService(ArendeHamtare hamtare, Kalender kalender) {
        this.hamtare = hamtare;
        this.kalender = kalender;
    }

    @Transactional
    public ArendeResponse delge(long id, DelgivningRequest request) {
        Betalningsforelaggande bf = hamta(id);
        bf.delge(request.datum(), kalender.idag());
        return spara(bf);
    }

    @Transactional
    public ArendeResponse bestrid(long id, BestridandeRequest request) {
        Betalningsforelaggande bf = hamta(id);
        bf.bestrid(request.datum(), request.grund(), kalender.idag());
        return spara(bf);
    }

    @Transactional
    public ArendeResponse meddelaUtslag(long id) {
        Betalningsforelaggande bf = hamta(id);
        bf.meddelaUtslag(kalender.idag());
        return spara(bf);
    }

    @Transactional
    public ArendeResponse overlamnaTillTingsratt(long id) {
        Betalningsforelaggande bf = hamta(id);
        bf.overlamnaTillTingsratt(kalender.idag());
        return spara(bf);
    }

    /// Skapar ett nytt utsökningsärende och returnerar det.
    @Transactional
    public ArendeResponse verkstall(long id, VerkstallighetRequest request) {
        Betalningsforelaggande bf = hamta(id);
        Utsokningsarende utsokning = bf.verkstall(request.handlaggare().strip(), kalender.idag());
        hamtare.spara(bf);
        return spara(utsokning);
    }

    private Betalningsforelaggande hamta(long id) {
        return hamtare.hamta(id, Betalningsforelaggande.class);
    }

    private ArendeResponse spara(Arende arende) {
        return ArendeMapper.tillResponse(hamtare.spara(arende));
    }
}
