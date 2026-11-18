package se.jtutala.handlaggning.arende;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;

/// Ärendets **dagbok**. Enligt utsökningsförordningen ska Kronofogden föra dagbok i varje mål.
///
/// Ansvar: spara händelser i ordning. Dagboken kan bara läggas till i – aldrig ändras.
@Embeddable
class Dagbok {

    @ElementCollection
    @CollectionTable(name = "arende_dagbok", joinColumns = @JoinColumn(name = "arende_id"))
    @OrderColumn(name = "rad")
    private List<Dagboksanteckning> anteckningar = new ArrayList<>();

    void anteckna(LocalDate datum, String handelse) {
        anteckningar.add(new Dagboksanteckning(datum, handelse));
    }

    List<Dagboksanteckning> anteckningar() {
        return List.copyOf(anteckningar);
    }
}
