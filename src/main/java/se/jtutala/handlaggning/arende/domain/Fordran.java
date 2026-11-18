package se.jtutala.handlaggning.arende.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import se.jtutala.handlaggning.common.RegelbrottException;

/// **Fordran** – kravet som ärendet gäller: vem som ska ha betalt, hur mycket och när det förföll.
///
/// Ett värdeobjekt: oföränderligt och alltid giltigt (beloppet kontrolleras när det skapas).
@Embeddable
public record Fordran(
        @Column(nullable = false, length = 200) String borgenar,
        @Column(nullable = false, precision = 12, scale = 2) BigDecimal kapitalbelopp,
        @Column(nullable = false) LocalDate forfallodag,
        @Column(nullable = false) boolean konsumentfordran) {

    public Fordran {
        if (kapitalbelopp.signum() <= 0) {
            throw new RegelbrottException("Skulden måste vara större än 0 kr");
        }
    }

    public boolean harForfallit(LocalDate idag) {
        return !forfallodag.isAfter(idag);
    }
}
