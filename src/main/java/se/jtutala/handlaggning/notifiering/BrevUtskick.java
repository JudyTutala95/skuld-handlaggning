package se.jtutala.handlaggning.notifiering;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import se.jtutala.handlaggning.arende.ArendeStatusAndrad;

/// Skickar brev till gäldenären när något viktigt händer i ett ärende.
///
/// `@ApplicationModuleListener` (Spring Modulith) =
/// `@TransactionalEventListener` + `@Async` + egen transaktion:
/// - körs först **efter** att ärendets transaktion committats (inga brev om något gick fel),
/// - körs **asynkront** – handläggaren behöver inte vänta på utskicket,
/// - fel här påverkar inte ärendet.
///
/// Här loggas brevet bara. I ett riktigt system skulle det t.ex. gå till en tryckeritjänst
/// eller en digital brevlåda.
@Component
class BrevUtskick {

    private static final Logger log = LoggerFactory.getLogger(BrevUtskick.class);

    @ApplicationModuleListener
    void nar(ArendeStatusAndrad handelse) {
        Brevmall.textFor(handelse).ifPresent(text ->
                log.info("📬 Brev till gäldenär #{}: {}", handelse.galdenarId(), text));
    }
}
