package se.jtutala.handlaggning.galdenar;

import java.time.DateTimeException;
import java.time.LocalDate;

/// Ett svenskt personnummer som **värdeobjekt**: kan inte skapas i ogiltigt skick.
///
/// Lagras som 12 siffror: `ÅÅÅÅMMDDNNNK`.
/// - `NNN` = födelsenummer, `K` = kontrollsiffra beräknad med Luhn-algoritmen.
/// - Samordningsnummer (för personer som inte är folkbokförda) har dag + 60, t.ex. `19850375`.
///
/// Personnummer är känsliga personuppgifter (GDPR). Använd `maskerat()` i loggar och listor,
/// och skicka dem aldrig i URL:er – de hamnar då i loggar och webbläsarhistorik.
public record Personnummer(String varde) {

    public Personnummer {
        if (!varde.matches("\\d{12}")) {
            throw new IllegalArgumentException("personnummer ska ha 12 siffror (ÅÅÅÅMMDD-NNNN)");
        }
        fodelsedatum(varde); // kastar om datumet är ogiltigt
        if (!luhnKorrekt(varde.substring(2))) {
            throw new IllegalArgumentException("personnumrets kontrollsiffra stämmer inte");
        }
    }

    /// Tolkar `ÅÅÅÅMMDD-NNNN` eller `ÅÅÅÅMMDDNNNN`.
    public static Personnummer parse(String text) {
        return new Personnummer(text.strip().replace("-", ""));
    }

    public String formaterat() {
        return varde.substring(0, 8) + "-" + varde.substring(8);
    }

    /// `19850315-****` – för listor och loggar.
    public String maskerat() {
        return varde.substring(0, 8) + "-****";
    }

    public LocalDate fodelsedatum() {
        return fodelsedatum(varde);
    }

    public boolean arSamordningsnummer() {
        return Integer.parseInt(varde.substring(6, 8)) > 60;
    }

    private static LocalDate fodelsedatum(String varde) {
        int ar = Integer.parseInt(varde.substring(0, 4));
        int manad = Integer.parseInt(varde.substring(4, 6));
        int dag = Integer.parseInt(varde.substring(6, 8));
        if (dag > 60) dag -= 60; // samordningsnummer
        try {
            return LocalDate.of(ar, manad, dag);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("personnumret innehåller ett ogiltigt datum");
        }
    }

    /// Luhn-algoritmen på de 10 sista siffrorna:
    /// varannan siffra (från vänster, start på första) dubbleras, siffersumman ska vara delbar med 10.
    static boolean luhnKorrekt(String tioSiffror) {
        int summa = 0;
        for (int i = 0; i < tioSiffror.length(); i++) {
            int siffra = tioSiffror.charAt(i) - '0';
            int produkt = (i % 2 == 0) ? siffra * 2 : siffra;
            summa += produkt > 9 ? produkt - 9 : produkt;
        }
        return summa % 10 == 0;
    }

    @Override
    public String toString() {
        return maskerat(); // läck aldrig hela numret via toString (loggar!)
    }
}
