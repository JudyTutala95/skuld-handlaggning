package se.jtutala.handlaggning.arende.domain;

import static se.jtutala.handlaggning.arende.Arendestatus.AVSKRIVET;
import static se.jtutala.handlaggning.arende.Arendestatus.BETALT;
import static se.jtutala.handlaggning.arende.Arendestatus.LONEUTMATNING;
import static se.jtutala.handlaggning.arende.Arendestatus.PRESKRIBERAT;
import static se.jtutala.handlaggning.arende.Arendestatus.REGISTRERAT;
import static se.jtutala.handlaggning.arende.Arendestatus.UTREDNING;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;

import org.jspecify.annotations.Nullable;

import se.jtutala.handlaggning.arende.Arendestatus;
import se.jtutala.handlaggning.arende.Arendetyp;
import se.jtutala.handlaggning.common.RegelbrottException;
import se.jtutala.handlaggning.regelverk.Maltyp;
import se.jtutala.handlaggning.regelverk.Preskription;

/// **Utsökning** – Kronofogden driver in en fastställd skuld.
///
/// Gäldenärens inkomster och tillgångar utreds. Löneutmätning innebär att arbetsgivaren
/// drar en del av lönen. Ärendet avslutas när skulden är betald, preskriberad eller avskriven.
///
/// ```
/// REGISTRERAT ─▶ UTREDNING ─▶ LONEUTMATNING ─▶ BETALT
///    (alla aktiva steg kan också gå till BETALT, PRESKRIBERAT eller AVSKRIVET)
/// ```
@Entity
@DiscriminatorValue("UTSOKNING")
public class Utsokningsarende extends Arende {

    static final Statusflode FLODE = Statusflode.av(Map.of(
            REGISTRERAT, Set.of(UTREDNING, BETALT, PRESKRIBERAT, AVSKRIVET),
            UTREDNING, Set.of(LONEUTMATNING, BETALT, PRESKRIBERAT, AVSKRIVET),
            LONEUTMATNING, Set.of(BETALT, PRESKRIBERAT, AVSKRIVET)));

    private @Nullable BigDecimal loneutmatningPerManad;

    /// Betalningsföreläggandet som ärendet startades från, om något.
    private @Nullable Long ursprungsarendeId;

    @Embedded
    private Inbetalningar inbetalningar = new Inbetalningar();

    /// Endast för JPA.
    protected Utsokningsarende() {
    }

    Utsokningsarende(Maltyp maltyp, long galdenarId, Fordran fordran, String handlaggare, LocalDate idag) {
        super(maltyp, galdenarId, fordran, handlaggare, idag);
    }

    static Utsokningsarende franUtslag(Betalningsforelaggande utslag, String handlaggare, LocalDate idag) {
        var arende = new Utsokningsarende(Maltyp.ENSKILT, utslag.getGaldenarId(), utslag.getFordran(), handlaggare, idag);
        arende.ursprungsarendeId = utslag.getId();
        return arende;
    }

    @Override
    public Arendetyp typ() {
        return Arendetyp.UTSOKNING;
    }

    @Override
    protected Statusflode statusflode() {
        return FLODE;
    }

    /// En inbetalning är ett erkännande av skulden och avbryter preskriptionen.
    @Override
    protected @Nullable LocalDate senastePreskriptionsavbrott() {
        return inbetalningar.senasteDatum();
    }

    /// Beslut om löneutmätning. Beloppet räknas fram av regelverket (förbehållsbeloppet).
    public void beslutaLoneutmatning(BigDecimal beloppPerManad, LocalDate idag) {
        kontrolleraUtmatningsbart(beloppPerManad);
        this.loneutmatningPerManad = beloppPerManad;
        bytStatus(LONEUTMATNING, idag, "Beslut om löneutmätning: %s kr per månad.".formatted(beloppPerManad.toPlainString()));
    }

    /// Registrerar en betalning. När allt är betalt avslutas ärendet automatiskt.
    public void registreraInbetalning(Inbetalning inbetalning, LocalDate idag) {
        kontrolleraAktivt();
        kontrolleraInteOverbetalning(inbetalning.belopp());
        inbetalningar.registrera(inbetalning);
        anteckna(idag, "Inbetalning %s kr (%s). Kvar: %s kr.".formatted(
                inbetalning.belopp().toPlainString(), inbetalning.datum(), kvarstaendeSkuld().toPlainString()));
        avslutaOmBetalt(idag);
    }

    public void markeraPreskriberat(LocalDate idag) {
        kontrolleraPreskriberat(idag);
        bytStatus(PRESKRIBERAT, idag, "Skulden preskriberades %s.".formatted(preskriptionsdag()));
    }

    public BigDecimal inbetaltTotalt() {
        return inbetalningar.summa();
    }

    public BigDecimal kvarstaendeSkuld() {
        return getFordran().kapitalbelopp().subtract(inbetaltTotalt());
    }

    public @Nullable BigDecimal getLoneutmatningPerManad() { return loneutmatningPerManad; }
    public @Nullable Long getUrsprungsarendeId() { return ursprungsarendeId; }
    public List<Inbetalning> getInbetalningar() { return inbetalningar.lista(); }

    // ---------- Regler ----------

    private void avslutaOmBetalt(LocalDate idag) {
        if (kvarstaendeSkuld().signum() == 0) {
            bytStatus(BETALT, idag, "Skulden är fullt betald.");
        }
    }

    private void kontrolleraUtmatningsbart(BigDecimal belopp) {
        if (belopp.signum() <= 0) {
            throw new RegelbrottException("Gäldenärens inkomst understiger förbehållsbeloppet – inget kan utmätas");
        }
    }

    private void kontrolleraInteOverbetalning(BigDecimal belopp) {
        if (belopp.compareTo(kvarstaendeSkuld()) > 0) {
            throw new RegelbrottException("Inbetalningen (%s kr) är större än kvarstående skuld (%s kr)"
                    .formatted(belopp.toPlainString(), kvarstaendeSkuld().toPlainString()));
        }
    }

    private void kontrolleraPreskriberat(LocalDate idag) {
        if (!Preskription.arPreskriberad(preskriptionsdag(), idag)) {
            throw new RegelbrottException("Skulden preskriberas först efter " + preskriptionsdag());
        }
    }
}
