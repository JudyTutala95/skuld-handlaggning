package se.jtutala.handlaggning.arende.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;

import org.jspecify.annotations.Nullable;

/// Ansvar: hålla ordning på **inbetalningarna** i ett utsökningsärende och summera dem.
@Embeddable
class Inbetalningar {

    @ElementCollection
    @CollectionTable(name = "arende_inbetalning", joinColumns = @JoinColumn(name = "arende_id"))
    @OrderColumn(name = "rad")
    private List<Inbetalning> lista = new ArrayList<>();

    void registrera(Inbetalning inbetalning) {
        lista.add(inbetalning);
    }

    BigDecimal summa() {
        return lista.stream().map(Inbetalning::belopp).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Nullable LocalDate senasteDatum() {
        return lista.isEmpty() ? null : lista.getLast().datum();
    }

    List<Inbetalning> lista() {
        return List.copyOf(lista);
    }
}
