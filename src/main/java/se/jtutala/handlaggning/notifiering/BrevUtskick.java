package se.jtutala.handlaggning.notifiering;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import se.jtutala.handlaggning.arende.ArendeStatusAndrad;

/// Ansvar: **reagera** på statusbyten och se till att rätt brev skickas.
///
/// - *Vilken text* → `Brevmall`
/// - *Hur det levereras* → `Brevlada`
///
/// `@ApplicationModuleListener` (Spring Modulith) körs asynkront, efter att ärendets
/// transaktion committats och i en egen transaktion.
@Component
class BrevUtskick {

    private final Brevlada brevlada;

    BrevUtskick(Brevlada brevlada) {
        this.brevlada = brevlada;
    }

    @ApplicationModuleListener
    void nar(ArendeStatusAndrad handelse) {
        Brevmall.textFor(handelse).ifPresent(text -> brevlada.skicka(handelse.galdenarId(), text));
    }
}
