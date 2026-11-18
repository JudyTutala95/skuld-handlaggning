package se.jtutala.handlaggning.arende;

/// **Domänhändelse** som publiceras varje gång ett ärende byter status.
///
/// Andra moduler kan lyssna utan att ärendemodulen känner till dem (lös koppling).
/// Exempel: `notifiering` skickar brev till gäldenären.
public record ArendeStatusAndrad(
        long arendeId,
        String arendenummer,
        long galdenarId,
        Arendetyp typ,
        Arendestatus fran,
        Arendestatus till) {
}
