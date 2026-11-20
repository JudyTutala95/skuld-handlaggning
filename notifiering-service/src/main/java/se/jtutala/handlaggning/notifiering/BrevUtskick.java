package se.jtutala.handlaggning.notifiering;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/// Ansvar: **reagera** på statusbyten från Kafka och se till att rätt brev skickas.
///
/// - *Vilken text* → `Brevmall`
/// - *Hur det levereras* → `Brevlada`
///
/// Meddelandet tas emot som text och läses med Jackson. Då behöver tjänsten inte känna till
/// ärendetjänstens Java-klasser, bara JSON-formatet.
///
/// `groupId`: startar man flera instanser av tjänsten delar de på meddelandena, och varje
/// brev skickas bara en gång.
@Component
class BrevUtskick {

    private static final Logger log = LoggerFactory.getLogger(BrevUtskick.class);

    private final Brevlada brevlada;
    private final JsonMapper json;

    BrevUtskick(Brevlada brevlada, JsonMapper json) {
        this.brevlada = brevlada;
        this.json = json;
    }

    @KafkaListener(topics = "arende-status", groupId = "notifiering")
    void nar(String meddelande) {
        ArendeStatusAndrad handelse;
        try {
            handelse = json.readValue(meddelande, ArendeStatusAndrad.class);
        } catch (JacksonException e) {
            // Ett trasigt meddelande ska inte stoppa alla som kommer efter det
            log.warn("Kunde inte läsa händelsen, hoppar över den: {}", meddelande, e);
            return;
        }
        Brevmall.textFor(handelse).ifPresent(text -> brevlada.skicka(handelse.galdenarId(), text));
    }
}
