package se.jtutala.handlaggning.galdenar;

import java.math.BigDecimal;
import java.util.List;

/// Svar: en gäldenär som den visas i API:t.
public record GaldenarSvar(
        Long id,
        String personnummer,
        String namn,
        String adress,
        BigDecimal nettoinkomstPerManad,
        BigDecimal boendekostnadPerManad,
        boolean sammanlevande,
        List<Integer> barnensFodelsear) {
}
