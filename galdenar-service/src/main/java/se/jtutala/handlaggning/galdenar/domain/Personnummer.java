package se.jtutala.handlaggning.galdenar.domain;

import java.time.DateTimeException;
import java.time.LocalDate;

/// Ett svenskt personnummer som **värdeobjekt**: kan inte skapas i ogiltigt skick.
///
/// Lagras som 12 siffror: `ÅÅÅÅMMDDNNNK` där `K` är en kontrollsiffra (se `Luhn`).
/// Samordningsnummer har dag + 60, t.ex. `19850375`.
///
/// Personnummer är känsliga personuppgifter. `toString()` maskerar därför numret.
public record Personnummer(String varde) {

    private static final int SAMORDNINGSTILLAGG = 60;

    public Personnummer {
        kontrolleraFormat(varde);
        fodelsedatum(varde);
        kontrolleraKontrollsiffra(varde);
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
        return dagsiffror(varde) > SAMORDNINGSTILLAGG;
    }

    @Override
    public String toString() {
        return maskerat();
    }

    private static void kontrolleraFormat(String varde) {
        if (!varde.matches("\\d{12}")) {
            throw new IllegalArgumentException("personnummer ska ha 12 siffror (ÅÅÅÅMMDD-NNNN)");
        }
    }

    private static void kontrolleraKontrollsiffra(String varde) {
        if (!Luhn.arGiltigt(varde.substring(2))) {
            throw new IllegalArgumentException("personnumrets kontrollsiffra stämmer inte");
        }
    }

    private static LocalDate fodelsedatum(String varde) {
        int dag = dagsiffror(varde) % SAMORDNINGSTILLAGG;
        try {
            return LocalDate.of(Integer.parseInt(varde.substring(0, 4)), Integer.parseInt(varde.substring(4, 6)), dag);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("personnumret innehåller ett ogiltigt datum");
        }
    }

    private static int dagsiffror(String varde) {
        return Integer.parseInt(varde.substring(6, 8));
    }
}
