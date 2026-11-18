package se.jtutala.handlaggning.arende.domain;

import static se.jtutala.handlaggning.arende.Arendestatus.AVSKRIVET;
import static se.jtutala.handlaggning.arende.Arendestatus.REGISTRERAT;
import static se.jtutala.handlaggning.arende.Arendestatus.SKULDSANERING_AVSLAG;
import static se.jtutala.handlaggning.arende.Arendestatus.SKULDSANERING_PAGAR;
import static se.jtutala.handlaggning.arende.Arendestatus.SKULDSANERING_SLUTFORD;
import static se.jtutala.handlaggning.arende.Arendestatus.UTREDNING;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import org.jspecify.annotations.Nullable;

import se.jtutala.handlaggning.arende.Arendestatus;
import se.jtutala.handlaggning.arende.Arendetyp;
import se.jtutala.handlaggning.common.RegelbrottException;
import se.jtutala.handlaggning.regelverk.Maltyp;

/// **Skuldsanering** – för den som inte kan betala sina skulder inom överskådlig tid.
/// En beviljad skuldsanering pågår i fem år, därefter är skulderna som ingick borta.
///
/// ```
/// REGISTRERAT ─▶ UTREDNING ─┬─▶ SKULDSANERING_PAGAR ─▶ SKULDSANERING_SLUTFORD  (efter 5 år)
///                           └─▶ SKULDSANERING_AVSLAG
/// ```
@Entity
@DiscriminatorValue("SKULDSANERING")
public class Skuldsaneringsarende extends Arende {

    static final Statusflode FLODE = Statusflode.av(Map.of(
            REGISTRERAT, Set.of(UTREDNING, AVSKRIVET),
            UTREDNING, Set.of(SKULDSANERING_PAGAR, SKULDSANERING_AVSLAG, AVSKRIVET),
            SKULDSANERING_PAGAR, Set.of(SKULDSANERING_SLUTFORD)));

    @Column(name = "skuldsanering_beviljad")
    private @Nullable LocalDate beviljad;

    /// Endast för JPA.
    protected Skuldsaneringsarende() {
    }

    Skuldsaneringsarende(Maltyp maltyp, long galdenarId, Fordran fordran, String handlaggare, LocalDate idag) {
        super(maltyp, galdenarId, fordran, handlaggare, idag);
    }

    @Override
    public Arendetyp typ() {
        return Arendetyp.SKULDSANERING;
    }

    @Override
    protected Statusflode statusflode() {
        return FLODE;
    }

    public void bevilja(String motivering, LocalDate idag) {
        this.beviljad = idag;
        bytStatus(SKULDSANERING_PAGAR, idag, "Skuldsanering beviljad, pågår till %s. %s"
                .formatted(new Skuldsaneringsperiod(idag).slutar(), motivering));
    }

    public void avsla(String motivering, LocalDate idag) {
        bytStatus(SKULDSANERING_AVSLAG, idag, "Ansökan avslagen. " + motivering);
    }

    public void slutfor(LocalDate idag) {
        kontrolleraAttPeriodenArSlut(idag);
        bytStatus(SKULDSANERING_SLUTFORD, idag, "Skuldsaneringen är slutförd. Skulderna som ingick är avskrivna.");
    }

    public @Nullable Skuldsaneringsperiod period() {
        return beviljad == null ? null : new Skuldsaneringsperiod(beviljad);
    }

    private void kontrolleraAttPeriodenArSlut(LocalDate idag) {
        Skuldsaneringsperiod period = period();
        if (period == null) {
            throw new RegelbrottException("Skuldsaneringen är inte beviljad");
        }
        if (!period.arAvslutad(idag)) {
            throw new RegelbrottException("Skuldsaneringen pågår till " + period.slutar());
        }
    }
}
