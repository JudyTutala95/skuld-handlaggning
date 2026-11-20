package se.jtutala.handlaggning.galdenar.dto;

import java.math.BigDecimal;
import java.util.List;

/// Svar: en gäldenär som den visas i API:t.
public record GaldenarResponse(
        Long id,
        String personnummer,
        String namn,
        String adress,
        BigDecimal nettoinkomstPerManad,
        BigDecimal boendekostnadPerManad,
        boolean sammanlevande,
        List<Integer> barnensFodelsear) {
}
