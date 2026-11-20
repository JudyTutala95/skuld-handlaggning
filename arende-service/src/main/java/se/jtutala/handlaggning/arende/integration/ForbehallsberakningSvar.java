package se.jtutala.handlaggning.arende.integration;

import java.math.BigDecimal;

/// Den del av gäldenärstjänstens svar som ärendetjänsten behöver.
///
/// **Tolerant läsare:** vi läser bara fälten vi använder. Lägger gäldenärstjänsten till
/// fler fält i svaret påverkas inte ärendetjänsten.
record ForbehallsberakningSvar(BigDecimal forbehallsbelopp, BigDecimal utmatningsbartBelopp) {
}
