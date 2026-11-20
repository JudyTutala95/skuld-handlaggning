package se.jtutala.handlaggning.arende.domain;

import static se.jtutala.handlaggning.arende.Arendestatus.AVSKRIVET;
import static se.jtutala.handlaggning.arende.Arendestatus.BESTRITT;
import static se.jtutala.handlaggning.arende.Arendestatus.DELGIVET;
import static se.jtutala.handlaggning.arende.Arendestatus.OVERLAMNAT_TILL_TINGSRATT;
import static se.jtutala.handlaggning.arende.Arendestatus.REGISTRERAT;
import static se.jtutala.handlaggning.arende.Arendestatus.UTSLAG_MEDDELAT;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import org.jspecify.annotations.Nullable;

import se.jtutala.handlaggning.arende.Arendestatus;
import se.jtutala.handlaggning.arende.Arendetyp;
import se.jtutala.handlaggning.arende.common.RegelbrottException;
import se.jtutala.handlaggning.arende.regelverk.Maltyp;

/// **Betalningsföreläggande** – en snabb, förenklad process för att fastställa att en skuld finns.
///
/// Borgenären ansöker, gäldenären delges och har 10 dagar att bestrida. Utan bestridande
/// meddelas ett *utslag* (samma verkan som en dom), som kan verkställas.
///
/// ```
/// REGISTRERAT ─▶ DELGIVET ─┬─▶ UTSLAG_MEDDELAT           (inget bestridande inom fristen)
///                          └─▶ BESTRITT ─┬─▶ OVERLAMNAT_TILL_TINGSRATT
///                                        └─▶ AVSKRIVET
/// ```
@Entity
@DiscriminatorValue("BETALNINGSFORELAGGANDE")
public class Betalningsforelaggande extends Arende {

    static final Statusflode FLODE = Statusflode.av(Map.of(
            REGISTRERAT, Set.of(DELGIVET, AVSKRIVET),
            DELGIVET, Set.of(BESTRITT, UTSLAG_MEDDELAT, AVSKRIVET),
            BESTRITT, Set.of(OVERLAMNAT_TILL_TINGSRATT, AVSKRIVET)));

    private @Nullable LocalDate delgivningsdatum;

    /// Endast för JPA.
    protected Betalningsforelaggande() {
    }

    Betalningsforelaggande(Maltyp maltyp, long galdenarId, Fordran fordran, String handlaggare, LocalDate idag) {
        kontrolleraEnskiltMal(maltyp);   // Java 25: satser före super() är tillåtna (JEP 513)
        super(maltyp, galdenarId, fordran, handlaggare, idag);
    }

    @Override
    public Arendetyp typ() {
        return Arendetyp.BETALNINGSFORELAGGANDE;
    }

    @Override
    protected Statusflode statusflode() {
        return FLODE;
    }

    @Override
    protected @Nullable LocalDate senastePreskriptionsavbrott() {
        return delgivningsdatum;
    }

    /// Gäldenären har tagit emot föreläggandet. Bestridandefristen börjar löpa.
    public void delge(LocalDate delgivningsdag, LocalDate idag) {
        kontrolleraInteIFramtiden(delgivningsdag, idag);
        this.delgivningsdatum = delgivningsdag;
        bytStatus(DELGIVET, idag, "Gäldenären delgiven %s. Sista dag att bestrida: %s."
                .formatted(delgivningsdag, frist().sistaDag()));
    }

    /// Gäldenären säger emot kravet – senast sista dagen i fristen.
    public void bestrid(LocalDate inkomDatum, String grund, LocalDate idag) {
        kontrolleraIntime(inkomDatum);
        bytStatus(BESTRITT, idag, "Bestridande inkom %s. Grund: %s".formatted(inkomDatum, grund));
    }

    /// Inget bestridande i tid → utslag. Skulden är nu fastställd.
    public void meddelaUtslag(LocalDate idag) {
        kontrolleraAttFristenGattUt(idag);
        bytStatus(UTSLAG_MEDDELAT, idag, "Utslag meddelat: gäldenären ska betala %s kr till %s."
                .formatted(getFordran().kapitalbelopp().toPlainString(), getFordran().borgenar()));
    }

    public void overlamnaTillTingsratt(LocalDate idag) {
        bytStatus(OVERLAMNAT_TILL_TINGSRATT, idag, "Borgenären begärde att målet överlämnas till tingsrätten.");
    }

    /// Ett utslag kan verkställas: skapar ett nytt utsökningsärende.
    public Utsokningsarende verkstall(String handlaggare, LocalDate idag) {
        kontrolleraUtslag();
        anteckna(idag, "Borgenären ansökte om verkställighet av utslaget.");
        return Utsokningsarende.franUtslag(this, handlaggare, idag);
    }

    public @Nullable Bestridandefrist bestridandefrist() {
        return delgivningsdatum == null ? null : new Bestridandefrist(delgivningsdatum);
    }

    public @Nullable LocalDate getDelgivningsdatum() {
        return delgivningsdatum;
    }

    // ---------- Regler ----------

    private Bestridandefrist frist() {
        Bestridandefrist frist = bestridandefrist();
        if (frist == null) {
            throw new RegelbrottException("Föreläggandet är inte delgivet ännu");
        }
        return frist;
    }

    private void kontrolleraIntime(LocalDate inkomDatum) {
        Bestridandefrist frist = frist();
        if (!frist.arIntime(inkomDatum)) {
            throw new RegelbrottException("Bestridandet kom in %s, efter fristen %s".formatted(inkomDatum, frist.sistaDag()));
        }
    }

    private void kontrolleraAttFristenGattUt(LocalDate idag) {
        Bestridandefrist frist = frist();
        if (!frist.harGattUt(idag)) {
            throw new RegelbrottException("Utslag kan meddelas först när bestridandefristen (%s) har gått ut"
                    .formatted(frist.sistaDag()));
        }
    }

    private void kontrolleraUtslag() {
        if (getStatus() != UTSLAG_MEDDELAT) {
            throw new RegelbrottException("Verkställighet kräver ett meddelat utslag");
        }
    }

    private static void kontrolleraInteIFramtiden(LocalDate delgivningsdag, LocalDate idag) {
        if (delgivningsdag.isAfter(idag)) {
            throw new RegelbrottException("Delgivningsdagen kan inte ligga i framtiden");
        }
    }

    private static void kontrolleraEnskiltMal(Maltyp maltyp) {
        if (maltyp == Maltyp.ALLMANT) {
            throw new RegelbrottException("Allmänna mål behöver inget betalningsföreläggande – de lämnas direkt för indrivning");
        }
    }
}
