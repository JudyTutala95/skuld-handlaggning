package se.jtutala.handlaggning.arende.dto;

import java.time.LocalDate;

/// Svar: en rad i ärendets dagbok.
public record DagboksanteckningResponse(LocalDate datum, String handelse) {
}
