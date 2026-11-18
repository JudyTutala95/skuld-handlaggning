package se.jtutala.handlaggning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/// Handläggningssystem för skuldärenden.
///
/// Applikationen är en **modulär monolit**: en enda deploybar enhet, men uppdelad i
/// moduler med tydliga gränser (ett paket per modul). Spring Modulith kontrollerar i
/// ett test (`ModulStrukturTest`) att modulerna inte använder varandras interna delar.
///
/// ```
/// galdenar ──┐
///            ├──▶ regelverk   (ren Java: normalbelopp, förbehållsbelopp, preskription)
/// arende ────┘
///   │  publicerar ArendeStatusAndrad
///   ▼
/// notifiering (skickar "brev" till gäldenären)
/// ```
@SpringBootApplication
public class HandlaggningApplication {

    static void main(String[] args) {
        SpringApplication.run(HandlaggningApplication.class, args);
    }
}
