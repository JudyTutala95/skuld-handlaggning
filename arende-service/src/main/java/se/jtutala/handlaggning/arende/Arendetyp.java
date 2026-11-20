package se.jtutala.handlaggning.arende;

/// De tre ärendetyperna. Varje typ har en egen klass med sina regler:
///
/// | Typ                      | Klass                     | Prefix i ärendenumret |
/// |--------------------------|---------------------------|-----------------------|
/// | Betalningsföreläggande   | `Betalningsforelaggande`  | BF                    |
/// | Utsökning                | `Utsokningsarende`        | UTS                   |
/// | Skuldsanering            | `Skuldsaneringsarende`    | SKS                   |
public enum Arendetyp {
    BETALNINGSFORELAGGANDE("BF"),
    UTSOKNING("UTS"),
    SKULDSANERING("SKS");

    private final String prefix;

    Arendetyp(String prefix) {
        this.prefix = prefix;
    }

    public String prefix() {
        return prefix;
    }
}
