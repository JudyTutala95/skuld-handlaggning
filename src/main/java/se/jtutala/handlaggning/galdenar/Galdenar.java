package se.jtutala.handlaggning.galdenar;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import org.jspecify.annotations.Nullable;

/// En **gäldenär** – personen som har skulden.
///
/// Ansvar: gäldenärens identitet och personuppgifter. Ekonomin är en egen klass (`Ekonomi`).
///
/// Package-private: andra moduler går via `GaldenarUppslag` och `GaldenarEkonomi`.
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

    @Embedded
    private Ekonomi ekonomi = Ekonomi.okand();

    @Version
    private long version;

    /// Endast för JPA.
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

    void bytEkonomi(Ekonomi ny) {
        this.ekonomi = ny;
    }

    String fulltNamn() {
        return fornamn + " " + efternamn;
    }

    @Nullable Long getId() { return id; }
    Personnummer getPersonnummer() { return personnummer; }
    String getAdress() { return adress; }
    Ekonomi getEkonomi() { return ekonomi; }
}
