package se.jtutala.handlaggning.arende;

import java.util.List;
import java.util.Map;
import java.util.Set;

/// Ansvar: svara på **"får ett ärende gå från status A till status B?"**
///
/// En enkel tillståndsmaskin. Varje ärendetyp har ett eget `Statusflode` (se konstanten `FLODE`
/// i `Betalningsforelaggande`, `Utsokningsarende` och `Skuldsaneringsarende`).
final class Statusflode {

    private final Map<Arendestatus, Set<Arendestatus>> overgangar;

    private Statusflode(Map<Arendestatus, Set<Arendestatus>> overgangar) {
        this.overgangar = Map.copyOf(overgangar);
    }

    static Statusflode av(Map<Arendestatus, Set<Arendestatus>> overgangar) {
        return new Statusflode(overgangar);
    }

    boolean tillater(Arendestatus fran, Arendestatus till) {
        return nastaFran(fran).contains(till);
    }

    List<Arendestatus> nastaFran(Arendestatus fran) {
        return overgangar.getOrDefault(fran, Set.of()).stream().sorted().toList();
    }
}
