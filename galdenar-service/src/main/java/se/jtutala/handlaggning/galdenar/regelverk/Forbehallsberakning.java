package se.jtutala.handlaggning.galdenar.regelverk;

import java.math.BigDecimal;

/// Resultatet av en förbehållsberäkning. Alla belopp per månad.
///
/// Att returnera hela uträkningen (inte bara slutsumman) gör att handläggaren
/// kan visa gäldenären *hur* beloppet räknats fram.
public record Forbehallsberakning(
        int normalbeloppsar,
        BigDecimal normalbelopp,
        BigDecimal boendekostnad,
        BigDecimal forbehallsbelopp,
        BigDecimal nettoinkomst,
        BigDecimal utmatningsbartBelopp) {

    public boolean finnsUtmatningsbartBelopp() {
        return utmatningsbartBelopp.signum() > 0;
    }
}
