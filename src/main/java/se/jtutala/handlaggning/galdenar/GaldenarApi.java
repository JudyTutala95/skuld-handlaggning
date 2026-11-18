package se.jtutala.handlaggning.galdenar;

import se.jtutala.handlaggning.regelverk.Forbehallsberakning;

/// Gäldenärsmodulens **publika API** – det enda andra moduler får använda.
///
/// Allt i underpaketen (`web`, `dto`, `service`, `domain`, `repository`) är internt.
/// Spring Modulith kontrollerar det i `ModulStrukturTest`.
public interface GaldenarApi {

    /// Kastar `HittadesInteException` om gäldenären inte finns.
    void kontrolleraAttFinns(long galdenarId);

    /// Hur mycket av gäldenärens lön som kan utmätas – med hela uträkningen.
    Forbehallsberakning forbehallsberakning(long galdenarId);
}
