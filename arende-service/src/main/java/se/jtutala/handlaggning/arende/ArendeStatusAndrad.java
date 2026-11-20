package se.jtutala.handlaggning.arende;

import org.springframework.modulith.events.Externalized;

/// **Händelse** som publiceras varje gång ett ärende byter status.
///
/// `@Externalized` gör att Spring Modulith skickar händelsen till Kafka-ämnet
/// `arende-status`, med ärendets id som nyckel. Samma nyckel hamnar alltid i samma
/// partition, så händelserna för ett ärende kommer fram i rätt ordning.
///
/// Händelsen är ett **kontrakt** med andra tjänster (t.ex. notifiering-service). Lägg gärna
/// till fält, men byt aldrig namn på eller ta bort ett fält – då går mottagarna sönder.
@Externalized(ArendeStatusAndrad.AMNE + "::#{arendeId()}")
public record ArendeStatusAndrad(
        long arendeId,
        String arendenummer,
        long galdenarId,
        Arendetyp typ,
        Arendestatus fran,
        Arendestatus till) {

    /// Kafka-ämnet (topic) som händelserna skickas till.
    public static final String AMNE = "arende-status";
}
