package se.jtutala.handlaggning.arende;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import se.jtutala.handlaggning.arende.ArendeDtos.ArendeRad;
import se.jtutala.handlaggning.arende.ArendeDtos.ArendeSvar;
import se.jtutala.handlaggning.arende.ArendeDtos.RegistreraArende;
import se.jtutala.handlaggning.common.HittadesInteException;
import se.jtutala.handlaggning.galdenar.GaldenarService;
import se.jtutala.handlaggning.regelverk.Forbehallsbelopp;
import se.jtutala.handlaggning.regelverk.Forbehallsberakning;
import se.jtutala.handlaggning.regelverk.Normalbelopp;

/// **Användningsfall** (use cases) för ärenden – en metod per sak handläggaren kan göra.
///
/// Servicen är medvetet tunn. Mönstret är alltid detsamma:
/// 1. hämta ärendet,
/// 2. anropa en metod på ärendet (där affärsreglerna finns),
/// 3. spara (då publiceras eventuella händelser),
/// 4. returnera ett svar (DTO).
///
/// Steg 1–4 är samlade i hjälpmetoden `utfor`.
@Service
@Transactional(readOnly = true)
public class ArendeService {

    private final ArendeRepository repository;
    private final GaldenarService galdenarService;
    private final Clock clock;

    ArendeService(ArendeRepository repository, GaldenarService galdenarService, Clock clock) {
        this.repository = repository;
        this.galdenarService = galdenarService;
        this.clock = clock;
    }

    // ---------- Läsa ----------

    public ArendeSvar hamta(long id) {
        return ArendeSvar.fran(arende(id));
    }

    public List<ArendeRad> sok(@Nullable Long galdenarId, @Nullable Arendestatus status) {
        return repository.sok(galdenarId, status).stream().map(ArendeRad::fran).toList();
    }

    // ---------- Skapa ----------

    @Transactional
    public ArendeSvar registrera(RegistreraArende k) {
        galdenarService.kontrolleraAttFinns(k.galdenarId());
        Arende arende = Arende.registrera(k.typ(), k.maltyp(), k.galdenarId(), k.borgenar().strip(),
                k.kapitalbelopp(), k.forfallodag(), k.arKonsumentfordran(), k.handlaggare().strip(), idag());
        return ArendeSvar.fran(repository.save(arende));
    }

    @Transactional
    public ArendeSvar begarVerkstallighet(long utslagsarendeId, String handlaggare) {
        Arende bf = arende(utslagsarendeId);
        Arende utsokning = bf.begarVerkstallighet(handlaggare.strip(), idag());
        repository.save(bf);
        return ArendeSvar.fran(repository.save(utsokning));
    }

    // ---------- Åtgärder ----------

    @Transactional
    public ArendeSvar delge(long id, LocalDate datum) {
        return utfor(id, a -> a.delge(datum, idag()));
    }

    @Transactional
    public ArendeSvar bestrid(long id, LocalDate datum, String grund) {
        return utfor(id, a -> a.bestrid(datum, grund, idag()));
    }

    @Transactional
    public ArendeSvar meddelaUtslag(long id) {
        return utfor(id, a -> a.meddelaUtslag(idag()));
    }

    @Transactional
    public ArendeSvar overlamnaTillTingsratt(long id) {
        return utfor(id, a -> a.overlamnaTillTingsratt(idag()));
    }

    @Transactional
    public ArendeSvar paborjaUtredning(long id) {
        return utfor(id, a -> a.paborjaUtredning(idag()));
    }

    /// Löneutmätning: regelverket räknar ut hur mycket som får tas av lönen.
    @Transactional
    public ArendeSvar beslutaLoneutmatning(long id) {
        return utfor(id, a -> {
            Forbehallsberakning berakning = Forbehallsbelopp.berakna(
                    galdenarService.hushall(a.getGaldenarId()), Normalbelopp.AR_2026);
            a.beslutaLoneutmatning(berakning.utmatningsbartBelopp(), idag());
        });
    }

    @Transactional
    public ArendeSvar registreraInbetalning(long id, BigDecimal belopp, LocalDate datum) {
        return utfor(id, a -> a.registreraInbetalning(belopp, datum, idag()));
    }

    @Transactional
    public ArendeSvar markeraPreskriberat(long id) {
        return utfor(id, a -> a.markeraPreskriberat(idag()));
    }

    @Transactional
    public ArendeSvar beslutaSkuldsanering(long id, boolean beviljas, String motivering) {
        return utfor(id, a -> {
            if (beviljas) a.beviljaSkuldsanering(motivering, idag());
            else a.avslaSkuldsanering(motivering, idag());
        });
    }

    @Transactional
    public ArendeSvar slutforSkuldsanering(long id) {
        return utfor(id, a -> a.slutforSkuldsanering(idag()));
    }

    @Transactional
    public ArendeSvar avskriv(long id, String orsak) {
        return utfor(id, a -> a.avskriv(orsak, idag()));
    }

    // ---------- Hjälpmetoder ----------

    private ArendeSvar utfor(long id, Consumer<Arende> atgard) {
        Arende arende = arende(id);
        atgard.accept(arende);
        // save() behövs för att Spring Data ska publicera händelserna från AbstractAggregateRoot
        return ArendeSvar.fran(repository.save(arende));
    }

    private Arende arende(long id) {
        return repository.findById(id).orElseThrow(() -> new HittadesInteException("Ärende", id));
    }

    private LocalDate idag() {
        return LocalDate.now(clock);
    }
}
