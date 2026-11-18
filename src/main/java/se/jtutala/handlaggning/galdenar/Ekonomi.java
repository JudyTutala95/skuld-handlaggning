package se.jtutala.handlaggning.galdenar;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;

import se.jtutala.handlaggning.regelverk.Hushall;

/// Gäldenärens **ekonomi och hushåll**: inkomst, boendekostnad, om personen bor med
/// någon och barnens födelseår. Bäddas in i `Galdenar` (`@Embeddable`).
///
/// Ansvar: hålla uppgifterna giltiga och översätta dem till regelverkets `Hushall`.
@Embeddable
class Ekonomi {

    /// Barn räknas som en del av hushållet upp till denna ålder.
    static final int HOGSTA_BARNALDER = 25;

    @Column(name = "nettoinkomst_per_manad", nullable = false)
    private BigDecimal nettoinkomstPerManad;

    @Column(name = "boendekostnad_per_manad", nullable = false)
    private BigDecimal boendekostnadPerManad;

    @Column(nullable = false)
    private boolean sammanlevande;

    @ElementCollection
    @CollectionTable(name = "galdenar_barn", joinColumns = @JoinColumn(name = "galdenar_id"))
    @OrderColumn(name = "rad")
    @Column(name = "fodelsear")
    private List<Integer> barnensFodelsear = new ArrayList<>();

    protected Ekonomi() {
        this(BigDecimal.ZERO, BigDecimal.ZERO, false, List.of());
    }

    private Ekonomi(BigDecimal nettoinkomst, BigDecimal boendekostnad, boolean sammanlevande, List<Integer> barnensFodelsear) {
        this.nettoinkomstPerManad = nettoinkomst;
        this.boendekostnadPerManad = boendekostnad;
        this.sammanlevande = sammanlevande;
        this.barnensFodelsear = new ArrayList<>(barnensFodelsear);
    }

    /// Ny gäldenär utan kända uppgifter.
    static Ekonomi okand() {
        return new Ekonomi();
    }

    /// Skapar en giltig ekonomi eller kastar `IllegalArgumentException`.
    static Ekonomi av(BigDecimal nettoinkomst, BigDecimal boendekostnad, boolean sammanlevande,
                      List<Integer> barnensFodelsear, int iAr) {
        kontrolleraFodelsear(barnensFodelsear, iAr);
        return new Ekonomi(nettoinkomst, boendekostnad, sammanlevande, barnensFodelsear);
    }

    /// Översätter till regelverkets begrepp. Barnens ålder räknas fram per `idag`.
    Hushall somHushall(LocalDate idag) {
        return new Hushall(nettoinkomstPerManad, boendekostnadPerManad, sammanlevande, barnensAldrar(idag));
    }

    BigDecimal nettoinkomstPerManad() { return nettoinkomstPerManad; }
    BigDecimal boendekostnadPerManad() { return boendekostnadPerManad; }
    boolean sammanlevande() { return sammanlevande; }
    List<Integer> barnensFodelsear() { return List.copyOf(barnensFodelsear); }

    private List<Integer> barnensAldrar(LocalDate idag) {
        return barnensFodelsear.stream().map(ar -> idag.getYear() - ar).toList();
    }

    private static void kontrolleraFodelsear(List<Integer> fodelsear, int iAr) {
        if (fodelsear.stream().anyMatch(ar -> ar > iAr || ar < iAr - HOGSTA_BARNALDER)) {
            throw new IllegalArgumentException("orimligt födelseår för barn");
        }
    }
}
