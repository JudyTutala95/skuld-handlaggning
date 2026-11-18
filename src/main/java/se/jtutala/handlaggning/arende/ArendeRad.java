package se.jtutala.handlaggning.arende;

import java.math.BigDecimal;

import org.jspecify.annotations.NullUnmarked;

/// Svar: ett ärende i en lista (utan dagbok).
@NullUnmarked
public record ArendeRad(Long id, String arendenummer, Arendetyp typ, Arendestatus status,
                        Long galdenarId, String borgenar, BigDecimal kvarstaendeSkuld, String handlaggare) {
}
