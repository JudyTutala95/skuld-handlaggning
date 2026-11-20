package se.jtutala.handlaggning.arende.dto;

import java.math.BigDecimal;

import org.jspecify.annotations.NullUnmarked;

import se.jtutala.handlaggning.arende.Arendestatus;
import se.jtutala.handlaggning.arende.Arendetyp;

/// Svar: ett ärende i en lista (utan dagbok).
@NullUnmarked
public record ArendeRadResponse(Long id, String arendenummer, Arendetyp typ, Arendestatus status,
                        Long galdenarId, String borgenar, BigDecimal kvarstaendeSkuld, String handlaggare) {
}
