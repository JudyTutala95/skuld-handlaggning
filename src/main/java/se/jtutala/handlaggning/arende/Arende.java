package se.jtutala.handlaggning.arende;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Version;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.AbstractAggregateRoot;

import se.jtutala.handlaggning.common.RegelbrottException;
import se.jtutala.handlaggning.regelverk.Maltyp;
import se.jtutala.handlaggning.regelverk.Preskription;

/// Ett **ärende** – systemets viktigaste begrepp (ett *aggregat* i DDD-termer).
///
/// ### Rik domänmodell
/// Affärsreglerna ligger HÄR, i metoder som `delge`, `bestrid`, `meddelaUtslag` ...
/// – inte utspridda i services. Servicen hämtar ärendet, anropar en metod och sparar.
/// Därför går reglerna att enhetstesta utan Spring och databas (se `ArendeTest`).
///
/// ### Regler som gäller alla metoder
/// - Statusbyten går alltid via `bytStatus`, som kontrollerar `Arendetyp`s tillståndsmaskin.
/// - Varje händelse skrivs i dagboken.
/// - Varje statusbyte registrerar en `ArendeStatusAndrad`-händelse. Spring Data publicerar den
///   automatiskt när ärendet sparas (`AbstractAggregateRoot`).
/// - Gäldenären refereras med **id**, inte med ett objekt – aggregat i olika moduler
///   pekar på varandra med id.
@Entity
public class Arende extends AbstractAggregateRoot<Arende> {

    /// Normal tid för att bestrida ett betalningsföreläggande, räknat från delgivningen.
    public static final int BESTRIDANDEFRIST_DAGAR = 10;
    /// En beviljad skuldsanering pågår i fem år.
    public static final int SKULDSANERING_AR = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @Nullable Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Arendetyp typ;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Arendestatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Maltyp maltyp;

    @Column(nullable = false)
    private long galdenarId;

    /// Den som har fordran, t.ex. "Skatteverket" eller "Inkasso AB".
    @Column(nullable = false, length = 200)
    private String borgenar;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal kapitalbelopp;

    @Column(nullable = false)
    private LocalDate forfallodag;

    /// Fordran mot en konsument för vara/tjänst – kortare preskriptionstid (3 år).
    private boolean konsumentfordran;

    @Column(nullable = false, length = 100)
    private String handlaggare;

    @Column(nullable = false)
    private LocalDate registrerad;

    private @Nullable LocalDate delgivningsdatum;
    private @Nullable BigDecimal loneutmatningPerManad;
    private @Nullable LocalDate skuldsaneringBeviljad;

    /// Utsökningsärenden som startats från ett utslag pekar tillbaka på betalningsföreläggandet.
    private @Nullable Long ursprungsarendeId;

    @ElementCollection
    @CollectionTable(name = "arende_inbetalning", joinColumns = @JoinColumn(name = "arende_id"))
    @OrderColumn(name = "rad")
    private List<Inbetalning> inbetalningar = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "arende_dagbok", joinColumns = @JoinColumn(name = "arende_id"))
    @OrderColumn(name = "rad")
    private List<Dagboksanteckning> dagbok = new ArrayList<>();

    @Version
    private long version;

    /// Endast för JPA.
    protected Arende() {
        this.typ = Arendetyp.UTSOKNING;
        this.status = Arendestatus.REGISTRERAT;
        this.maltyp = Maltyp.ENSKILT;
        this.borgenar = "";
        this.kapitalbelopp = BigDecimal.ZERO;
        this.forfallodag = LocalDate.MIN;
        this.handlaggare = "";
        this.registrerad = LocalDate.MIN;
    }

    private Arende(Arendetyp typ, Maltyp maltyp, long galdenarId, String borgenar, BigDecimal kapitalbelopp,
                   LocalDate forfallodag, boolean konsumentfordran, String handlaggare, LocalDate idag) {
        if (kapitalbelopp.signum() <= 0) {
            throw new RegelbrottException("Skulden måste vara större än 0 kr");
        }
        if (forfallodag.isAfter(idag)) {
            throw new RegelbrottException("Skulden har inte förfallit till betalning ännu (förfallodag " + forfallodag + ")");
        }
        if (typ == Arendetyp.BETALNINGSFORELAGGANDE && maltyp == Maltyp.ALLMANT) {
            throw new RegelbrottException("Allmänna mål behöver inget betalningsföreläggande – de lämnas direkt för indrivning");
        }
        this.typ = typ;
        this.maltyp = maltyp;
        this.status = Arendestatus.REGISTRERAT;
        this.galdenarId = galdenarId;
        this.borgenar = borgenar;
        this.kapitalbelopp = kapitalbelopp;
        this.forfallodag = forfallodag;
        this.konsumentfordran = konsumentfordran;
        this.handlaggare = handlaggare;
        this.registrerad = idag;
        anteckna(idag, "Ärendet registrerat (%s, %s mål). Borgenär: %s. Belopp: %s kr."
                .formatted(typ, maltyp.name().toLowerCase(), borgenar, kapitalbelopp.toPlainString()));
    }

    /// Fabriksmetod – det enda sättet att skapa ett nytt ärende.
    public static Arende registrera(Arendetyp typ, Maltyp maltyp, long galdenarId, String borgenar,
                                   BigDecimal kapitalbelopp, LocalDate forfallodag, boolean konsumentfordran,
                                   String handlaggare, LocalDate idag) {
        return new Arende(typ, maltyp, galdenarId, borgenar, kapitalbelopp, forfallodag, konsumentfordran, handlaggare, idag);
    }

    // =====================================================================
    //  Betalningsföreläggande
    // =====================================================================

    /// Gäldenären har tagit emot föreläggandet. Bestridandefristen börjar löpa.
    public void delge(LocalDate delgivningsdag, LocalDate idag) {
        kravTyp(Arendetyp.BETALNINGSFORELAGGANDE);
        if (delgivningsdag.isAfter(idag)) {
            throw new RegelbrottException("Delgivningsdagen kan inte ligga i framtiden");
        }
        this.delgivningsdatum = delgivningsdag;
        bytStatus(Arendestatus.DELGIVET, idag,
                "Gäldenären delgiven %s. Sista dag att bestrida: %s.".formatted(delgivningsdag, bestridandefrist()));
    }

    /// Gäldenären säger emot kravet. Måste ske senast sista dagen i fristen.
    public void bestrid(LocalDate inkomDatum, String grund, LocalDate idag) {
        kravTyp(Arendetyp.BETALNINGSFORELAGGANDE);
        LocalDate frist = bestridandefrist();
        if (frist != null && inkomDatum.isAfter(frist)) {
            throw new RegelbrottException("Bestridandet kom in %s, efter fristen %s".formatted(inkomDatum, frist));
        }
        bytStatus(Arendestatus.BESTRITT, idag, "Bestridande inkom %s. Grund: %s".formatted(inkomDatum, grund));
    }

    /// Inget bestridande kom in i tid → utslag. Skulden är nu fastställd (exekutionstitel).
    public void meddelaUtslag(LocalDate idag) {
        kravTyp(Arendetyp.BETALNINGSFORELAGGANDE);
        LocalDate frist = bestridandefrist();
        if (frist == null || !idag.isAfter(frist)) {
            throw new RegelbrottException("Utslag kan meddelas först när bestridandefristen (%s) har gått ut".formatted(frist));
        }
        bytStatus(Arendestatus.UTSLAG_MEDDELAT, idag, "Utslag meddelat: gäldenären ska betala %s kr till %s."
                .formatted(kapitalbelopp.toPlainString(), borgenar));
    }

    public void overlamnaTillTingsratt(LocalDate idag) {
        kravTyp(Arendetyp.BETALNINGSFORELAGGANDE);
        bytStatus(Arendestatus.OVERLAMNAT_TILL_TINGSRATT, idag, "Borgenären begärde att målet överlämnas till tingsrätten.");
    }

    /// Ett utslag kan verkställas: skapar ett nytt utsökningsärende (enskilt mål).
    public Arende begarVerkstallighet(String handlaggare, LocalDate idag) {
        kravTyp(Arendetyp.BETALNINGSFORELAGGANDE);
        if (status != Arendestatus.UTSLAG_MEDDELAT) {
            throw new RegelbrottException("Verkställighet kräver ett meddelat utslag");
        }
        Arende utsokning = registrera(Arendetyp.UTSOKNING, Maltyp.ENSKILT, galdenarId, borgenar,
                kapitalbelopp, forfallodag, konsumentfordran, handlaggare, idag);
        utsokning.ursprungsarendeId = id;
        anteckna(idag, "Borgenären ansökte om verkställighet av utslaget.");
        return utsokning;
    }

    // =====================================================================
    //  Utsökning och skuldsanering
    // =====================================================================

    public void paborjaUtredning(LocalDate idag) {
        bytStatus(Arendestatus.UTREDNING, idag, "Utredning av gäldenärens tillgångar och inkomster påbörjad.");
    }

    /// Beslut om löneutmätning. Beloppet räknas fram av regelverket (förbehållsbeloppet).
    public void beslutaLoneutmatning(BigDecimal beloppPerManad, LocalDate idag) {
        kravTyp(Arendetyp.UTSOKNING);
        if (beloppPerManad.signum() <= 0) {
            throw new RegelbrottException("Gäldenärens inkomst understiger förbehållsbeloppet – inget kan utmätas");
        }
        this.loneutmatningPerManad = beloppPerManad;
        bytStatus(Arendestatus.LONEUTMATNING, idag,
                "Beslut om löneutmätning: %s kr per månad.".formatted(beloppPerManad.toPlainString()));
    }

    /// Registrerar en betalning. När allt är betalt avslutas ärendet automatiskt.
    /// En inbetalning är också ett preskriptionsavbrott (gäldenären erkänner skulden).
    public void registreraInbetalning(BigDecimal belopp, LocalDate datum, LocalDate idag) {
        kravTyp(Arendetyp.UTSOKNING);
        kravAktivt();
        if (belopp.signum() <= 0) {
            throw new RegelbrottException("Inbetalningen måste vara större än 0 kr");
        }
        if (belopp.compareTo(kvarstaendeSkuld()) > 0) {
            throw new RegelbrottException("Inbetalningen (%s kr) är större än kvarstående skuld (%s kr)"
                    .formatted(belopp.toPlainString(), kvarstaendeSkuld().toPlainString()));
        }
        inbetalningar.add(new Inbetalning(belopp, datum));
        anteckna(idag, "Inbetalning %s kr (%s). Kvar: %s kr.".formatted(belopp.toPlainString(), datum, kvarstaendeSkuld().toPlainString()));
        if (kvarstaendeSkuld().signum() == 0) {
            bytStatus(Arendestatus.BETALT, idag, "Skulden är fullt betald.");
        }
    }

    public void markeraPreskriberat(LocalDate idag) {
        kravTyp(Arendetyp.UTSOKNING);
        if (!Preskription.arPreskriberad(preskriptionsdag(), idag)) {
            throw new RegelbrottException("Skulden preskriberas först efter " + preskriptionsdag());
        }
        bytStatus(Arendestatus.PRESKRIBERAT, idag, "Skulden preskriberades %s.".formatted(preskriptionsdag()));
    }

    public void beviljaSkuldsanering(String motivering, LocalDate idag) {
        kravTyp(Arendetyp.SKULDSANERING);
        this.skuldsaneringBeviljad = idag;
        bytStatus(Arendestatus.SKULDSANERING_PAGAR, idag,
                "Skuldsanering beviljad, pågår till %s. %s".formatted(skuldsaneringSlutar(), motivering));
    }

    public void avslaSkuldsanering(String motivering, LocalDate idag) {
        kravTyp(Arendetyp.SKULDSANERING);
        bytStatus(Arendestatus.SKULDSANERING_AVSLAG, idag, "Ansökan avslagen. " + motivering);
    }

    public void slutforSkuldsanering(LocalDate idag) {
        kravTyp(Arendetyp.SKULDSANERING);
        LocalDate slut = skuldsaneringSlutar();
        if (slut == null || idag.isBefore(slut)) {
            throw new RegelbrottException("Skuldsaneringen pågår till " + slut);
        }
        bytStatus(Arendestatus.SKULDSANERING_SLUTFORD, idag, "Skuldsaneringen är slutförd. Skulderna som ingick är avskrivna.");
    }

    /// Ärendet avslutas utan vidare åtgärd, t.ex. för att borgenären återkallat ansökan.
    public void avskriv(String orsak, LocalDate idag) {
        bytStatus(Arendestatus.AVSKRIVET, idag, "Ärendet avskrivet. Orsak: " + orsak);
    }

    // =====================================================================
    //  Beräknade värden
    // =====================================================================

    public BigDecimal inbetaltTotalt() {
        return inbetalningar.stream().map(Inbetalning::belopp).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal kvarstaendeSkuld() {
        return kapitalbelopp.subtract(inbetaltTotalt());
    }

    public @Nullable LocalDate bestridandefrist() {
        return delgivningsdatum == null ? null : delgivningsdatum.plusDays(BESTRIDANDEFRIST_DAGAR);
    }

    public @Nullable LocalDate skuldsaneringSlutar() {
        return skuldsaneringBeviljad == null ? null : skuldsaneringBeviljad.plusYears(SKULDSANERING_AR);
    }

    public LocalDate preskriptionsdag() {
        LocalDate senasteInbetalning = inbetalningar.isEmpty() ? null : inbetalningar.getLast().datum();
        LocalDate avbrott = senaste(senasteInbetalning, delgivningsdatum);
        return Preskription.preskriptionsdag(maltyp, forfallodag, konsumentfordran, avbrott);
    }

    /// Vilka statusar ärendet kan gå till härnäst – bra för ett gränssnitt som visar knappar.
    public List<Arendestatus> mojligaNastaStatusar() {
        return typ.tillatnaOvergangar(status).stream().sorted().toList();
    }

    /// Ärendenummer för människor, t.ex. `UTS-2026-000042`.
    public String arendenummer() {
        return "%s-%d-%06d".formatted(typ.prefix(), registrerad.getYear(), id == null ? 0 : id);
    }

    // =====================================================================
    //  Interna hjälpmetoder
    // =====================================================================

    /// Det ENDA stället där status ändras.
    private void bytStatus(Arendestatus ny, LocalDate idag, String dagbokstext) {
        if (!typ.tillatnaOvergangar(status).contains(ny)) {
            throw new RegelbrottException("Ett ärende av typen %s kan inte gå från '%s' till '%s'"
                    .formatted(typ, status.beskrivning(), ny.beskrivning()));
        }
        Arendestatus fran = status;
        status = ny;
        anteckna(idag, dagbokstext);
        registerEvent(new ArendeStatusAndrad(Objects.requireNonNull(id, "ärendet måste vara sparat"),
                arendenummer(), galdenarId, typ, fran, ny));
    }

    private void anteckna(LocalDate datum, String text) {
        dagbok.add(new Dagboksanteckning(datum, text));
    }

    private void kravTyp(Arendetyp kravd) {
        if (typ != kravd) {
            throw new RegelbrottException("Åtgärden gäller bara ärenden av typen " + kravd);
        }
    }

    private void kravAktivt() {
        if (status.avslutad()) {
            throw new RegelbrottException("Ärendet är avslutat (" + status.beskrivning() + ")");
        }
    }

    /// Händelser som väntar på att publiceras (för tester i samma paket).
    List<Object> vantandeHandelser() {
        return List.copyOf(domainEvents());
    }

    private static @Nullable LocalDate senaste(@Nullable LocalDate a, @Nullable LocalDate b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isAfter(b) ? a : b;
    }

    // --- Getters ---
    public @Nullable Long getId() { return id; }
    public Arendetyp getTyp() { return typ; }
    public Arendestatus getStatus() { return status; }
    public Maltyp getMaltyp() { return maltyp; }
    public long getGaldenarId() { return galdenarId; }
    public String getBorgenar() { return borgenar; }
    public BigDecimal getKapitalbelopp() { return kapitalbelopp; }
    public LocalDate getForfallodag() { return forfallodag; }
    public boolean isKonsumentfordran() { return konsumentfordran; }
    public String getHandlaggare() { return handlaggare; }
    public LocalDate getRegistrerad() { return registrerad; }
    public @Nullable LocalDate getDelgivningsdatum() { return delgivningsdatum; }
    public @Nullable BigDecimal getLoneutmatningPerManad() { return loneutmatningPerManad; }
    public @Nullable Long getUrsprungsarendeId() { return ursprungsarendeId; }
    public List<Inbetalning> getInbetalningar() { return List.copyOf(inbetalningar); }
    public List<Dagboksanteckning> getDagbok() { return List.copyOf(dagbok); }
    public long getVersion() { return version; }
}
