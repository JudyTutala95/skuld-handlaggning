package se.jtutala.handlaggning.notifiering;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/// Låtsas-leverans: skriver brevet i loggen. Byt mot en riktig implementation i produktion.
@Component
class LoggandeBrevlada implements Brevlada {

    private static final Logger log = LoggerFactory.getLogger(LoggandeBrevlada.class);

    @Override
    public void skicka(long galdenarId, String text) {
        log.info("📬 Brev till gäldenär #{}: {}", galdenarId, text);
    }
}
