package se.jtutala.handlaggning.galdenar;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Version;

import org.jspecify.annotations.Nullable;

import se.jtutala.handlaggning.regelverk.Hushall;

/// En **gäldenär** – personen som har skulden.
///
/// Klassen är package-private: andra moduler får inte röra entiteten direkt,
/// bara gå via `GaldenarService` (modulens publika API).
@Entity
class Galdenar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @Nullable Long id;

    @Column(nullable = false, unique = true, length = 12)
    private Personnummer personnummer;

    @Column(nullable = false)
    private String fornamn;

    @Column(nullable = false)
    private String efternamn;

    @Column(nullable = false)
    private String adress;

    // --- Ekonomi (används vid löneutmätning) ---
    @Column(nullable = false)
    private BigDecimal nettoinkomstPerManad = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal boendekostnadPerManad = BigDecimal.ZERO;

    private boolean sammanlevande;

    @ElementCollection
    @CollectionTable(name = "galdenar_barn", joinColumns = @JoinColumn(name = "galdenar_id"))
    @OrderColumn(name = "rad")
    @Column(name = "fodelsear")
    private List<Integer> barnensFodelsear = new ArrayList<>();

    @Version
    private long version;

    protected Galdenar() {
        this.personnummer = new Personnummer("191212121212");
        this.fornamn = "";
        this.efternamn = "";
        this.adress = "";
    }

    Galdenar(Personnummer personnummer, String fornamn, String efternamn, String adress) {
        this.personnummer = personnummer;
        this.fornamn = fornamn;
        this.efternamn = efternamn;
        this.adress = adress;
    }

    void uppdateraEkonomi(BigDecimal nettoinkomst, BigDecimal boendekostnad, boolean sammanlevande, List<Integer> barnensFodelsear) {
        this.nettoinkomstPerManad = nettoinkomst;
        this.boendekostnadPerManad = boendekostnad;
        this.sammanlevande = sammanlevande;
        this.barnensFodelsear.clear();
        this.barnensFodelsear.addAll(barnensFodelsear);
    }

    /// Översätter till regelverkets `Hushall`. Barnens ålder räknas fram per "idag".
    Hushall hushall(LocalDate idag) {
        List<Integer> aldrar = barnensFodelsear.stream().map(ar -> idag.getYear() - ar).toList();
        return new Hushall(nettoinkomstPerManad, boendekostnadPerManad, sammanlevande, aldrar);
    }

    @Nullable Long getId() { return id; }
    Personnummer getPersonnummer() { return personnummer; }
    String getFornamn() { return fornamn; }
    String getEfternamn() { return efternamn; }
    String getAdress() { return adress; }
    BigDecimal getNettoinkomstPerManad() { return nettoinkomstPerManad; }
    BigDecimal getBoendekostnadPerManad() { return boendekostnadPerManad; }
    boolean isSammanlevande() { return sammanlevande; }
    List<Integer> getBarnensFodelsear() { return List.copyOf(barnensFodelsear); }
}
