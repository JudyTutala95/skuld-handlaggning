package se.jtutala.handlaggning.arende;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import se.jtutala.handlaggning.common.Kalender;

/// Användningsfall för **betalningsförelägganden**: delge, bestrida, utslag, tingsrätt, verkställighet.
///
/// Varje metod följer samma mönster: hämta → anropa en metod på ärendet → spara → svara.
@Service
public class BetalningsforelaggandeHandlaggning {

    private final Arendelager lager;
    private final Kalender kalender;

    BetalningsforelaggandeHandlaggning(Arendelager lager, Kalender kalender) {
        this.lager = lager;
        this.kalender = kalender;
    }

    @Transactional
    public ArendeSvar delge(long id, LocalDate datum) {
        Betalningsforelaggande bf = hamta(id);
        bf.delge(datum, kalender.idag());
        return spara(bf);
    }

    @Transactional
    public ArendeSvar bestrid(long id, LocalDate datum, String grund) {
        Betalningsforelaggande bf = hamta(id);
        bf.bestrid(datum, grund, kalender.idag());
        return spara(bf);
    }

    @Transactional
    public ArendeSvar meddelaUtslag(long id) {
        Betalningsforelaggande bf = hamta(id);
        bf.meddelaUtslag(kalender.idag());
        return spara(bf);
    }

    @Transactional
    public ArendeSvar overlamnaTillTingsratt(long id) {
        Betalningsforelaggande bf = hamta(id);
        bf.overlamnaTillTingsratt(kalender.idag());
        return spara(bf);
    }

    /// Skapar ett nytt utsökningsärende och returnerar det.
    @Transactional
    public ArendeSvar verkstall(long id, String handlaggare) {
        Betalningsforelaggande bf = hamta(id);
        Utsokningsarende utsokning = bf.verkstall(handlaggare.strip(), kalender.idag());
        lager.spara(bf);
        return spara(utsokning);
    }

    private Betalningsforelaggande hamta(long id) {
        return lager.hamta(id, Betalningsforelaggande.class);
    }

    private ArendeSvar spara(Arende arende) {
        return ArendeSvarMapper.tillSvar(lager.spara(arende));
    }
}
