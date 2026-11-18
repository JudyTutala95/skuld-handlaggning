package se.jtutala.handlaggning.arende.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.AbstractAggregateRoot;

import se.jtutala.handlaggning.arende.ArendeStatusAndrad;
import se.jtutala.handlaggning.arende.Arendestatus;
import se.jtutala.handlaggning.arende.Arendetyp;
import se.jtutala.handlaggning.common.RegelbrottException;
import se.jtutala.handlaggning.regelverk.Maltyp;
import se.jtutala.handlaggning.regelverk.Preskription;

/// Basklass för alla ärenden. Ansvar: **det som är gemensamt för alla ärendetyper** –
/// identitet, status, dagbok och händelser.
///
/// Reglerna för respektive process finns i subklasserna:
///
/// ```
///                         Arende
///            ┌──────────────┼──────────────────┐
/// Betalningsforelaggande  Utsokningsarende  Skuldsaneringsarende
/// ```
///
/// - **Statusbyten** går alltid via `bytStatus`, som frågar subklassens `Statusflode`.
/// - Varje statusbyte skrivs i dagboken och registrerar en `ArendeStatusAndrad`-händelse,
///   som Spring Data publicerar när ärendet sparas (`AbstractAggregateRoot`).
/// - JPA lagrar alla typer i samma tabell (`SINGLE_TABLE`); kolumnen `typ` avgör vilken klass.
@Entity
@Table(name = "arende")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "typ", length = 30)
public abstract class Arende extends AbstractAggregateRoot<Arende> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @Nullable Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Arendestatus status = Arendestatus.REGISTRERAT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Maltyp maltyp = Maltyp.ENSKILT;

    @Column(nullable = false)
    private long galdenarId;

    @Embedded
    private Fordran fordran;

    @Column(nullable = false, length = 100)
    private String handlaggare = "";

    @Column(nullable = false)
    private LocalDate registrerad = LocalDate.MIN;

    @Embedded
    private Dagbok dagbok = new Dagbok();

    @Version
    private long version;

    /// Endast för JPA – Hibernate fyller fälten direkt efteråt.
    protected Arende() {
    }

    protected Arende(Maltyp maltyp, long galdenarId, Fordran fordran, String handlaggare, LocalDate idag) {
        kontrolleraForfallen(fordran, idag);
        this.maltyp = maltyp;
        this.galdenarId = galdenarId;
        this.fordran = fordran;
        this.handlaggare = handlaggare;
        this.registrerad = idag;
        anteckna(idag, registreringstext());
    }

    /// Vilken typ ärendet är. Varje subklass svarar med sin egen typ.
    public abstract Arendetyp typ();

    /// Vilka statusbyten ärendetypen tillåter.
    protected abstract Statusflode statusflode();

    /// Senaste händelse som avbryter preskriptionen. Subklasserna vet vad det är.
    protected @Nullable LocalDate senastePreskriptionsavbrott() {
        return null;
    }

    // ---------- Åtgärder som gäller flera typer ----------

    public void paborjaUtredning(LocalDate idag) {
        bytStatus(Arendestatus.UTREDNING, idag, "Utredning av gäldenärens tillgångar och inkomster påbörjad.");
    }

    public void avskriv(String orsak, LocalDate idag) {
        bytStatus(Arendestatus.AVSKRIVET, idag, "Ärendet avskrivet. Orsak: " + orsak);
    }

    // ---------- Frågor ----------

    public LocalDate preskriptionsdag() {
        return Preskription.preskriptionsdag(maltyp, fordran.forfallodag(), fordran.konsumentfordran(),
                senastePreskriptionsavbrott());
    }

    public List<Arendestatus> mojligaNastaStatusar() {
        return statusflode().nastaFran(status);
    }

    /// Ärendenummer för människor, t.ex. `UTS-2026-000042`.
    public String arendenummer() {
        return "%s-%d-%06d".formatted(typ().prefix(), registrerad.getYear(), id == null ? 0 : id);
    }

    // ---------- För subklasserna ----------

    /// Det ENDA stället där status ändras.
    protected final void bytStatus(Arendestatus ny, LocalDate idag, String dagbokstext) {
        kontrolleraOvergang(ny);
        Arendestatus fran = status;
        status = ny;
        anteckna(idag, dagbokstext);
        registerEvent(statusAndrad(fran, ny));
    }

    protected final void anteckna(LocalDate idag, String text) {
        dagbok.anteckna(idag, text);
    }

    protected final void kontrolleraAktivt() {
        if (status.avslutad()) {
            throw new RegelbrottException("Ärendet är avslutat (" + status.beskrivning() + ")");
        }
    }

    /// Händelser som väntar på att publiceras (för tester i samma paket).
    List<Object> vantandeHandelser() {
        return List.copyOf(domainEvents());
    }

    // ---------- Privata hjälpmetoder ----------

    private void kontrolleraOvergang(Arendestatus ny) {
        if (!statusflode().tillater(status, ny)) {
            throw new RegelbrottException("Ett ärende av typen %s kan inte gå från '%s' till '%s'"
                    .formatted(typ(), status.beskrivning(), ny.beskrivning()));
        }
    }

    private ArendeStatusAndrad statusAndrad(Arendestatus fran, Arendestatus till) {
        long sparatId = Objects.requireNonNull(id, "ärendet måste vara sparat");
        return new ArendeStatusAndrad(sparatId, arendenummer(), galdenarId, typ(), fran, till);
    }

    private String registreringstext() {
        return "Ärendet registrerat (%s, %s mål). Borgenär: %s. Belopp: %s kr.".formatted(
                typ(), maltyp.name().toLowerCase(), fordran.borgenar(), fordran.kapitalbelopp().toPlainString());
    }

    private static void kontrolleraForfallen(Fordran fordran, LocalDate idag) {
        if (!fordran.harForfallit(idag)) {
            throw new RegelbrottException("Skulden har inte förfallit till betalning ännu (förfallodag "
                    + fordran.forfallodag() + ")");
        }
    }

    // ---------- Getters ----------

    public @Nullable Long getId() { return id; }
    public Arendestatus getStatus() { return status; }
    public Maltyp getMaltyp() { return maltyp; }
    public long getGaldenarId() { return galdenarId; }
    public Fordran getFordran() { return fordran; }
    public String getHandlaggare() { return handlaggare; }
    public LocalDate getRegistrerad() { return registrerad; }
    public List<Dagboksanteckning> getDagbok() { return dagbok.anteckningar(); }
    public long getVersion() { return version; }
}
