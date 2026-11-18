package se.jtutala.handlaggning.arende;

import java.time.LocalDate;

/// Tiden gäldenären har på sig att **bestrida** ett betalningsföreläggande:
/// normalt 10 dagar från delgivningen.
public record Bestridandefrist(LocalDate delgivningsdag) {

    public static final int DAGAR = 10;

    public LocalDate sistaDag() {
        return delgivningsdag.plusDays(DAGAR);
    }

    public boolean arIntime(LocalDate inkomDatum) {
        return !inkomDatum.isAfter(sistaDag());
    }

    public boolean harGattUt(LocalDate idag) {
        return idag.isAfter(sistaDag());
    }
}
