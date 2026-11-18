package se.jtutala.handlaggning.regelverk;

import java.math.BigDecimal;

/// Normalbelopp per månad – det belopp som en person anses behöva för sina
/// levnadskostnader (mat, kläder, hygien, el, telefon osv.), **exklusive boende**.
///
/// Beloppen bestäms varje år av Kronofogden i en föreskrift.
/// Källa för 2026: KFMFS 2025:1, 2 §.
///
/// | Kategori                          | kr/månad |
/// |-----------------------------------|---------:|
/// | Ensamstående                      | 6 243    |
/// | Makar/sambor (tillsammans)        | 10 314   |
/// | Barn 0–6 år                       | 3 336    |
/// | Barn 7–10 år                      | 4 004    |
/// | Barn 11–14 år                     | 4 672    |
/// | Barn 15 år eller äldre            | 5 339    |
public record Normalbelopp(
        int ar,
        BigDecimal ensamstaende,
        BigDecimal sammanlevande,
        BigDecimal barn0till6,
        BigDecimal barn7till10,
        BigDecimal barn11till14,
        BigDecimal barn15ochAldre) {

    public static final Normalbelopp AR_2026 = new Normalbelopp(2026,
            kr(6243), kr(10314), kr(3336), kr(4004), kr(4672), kr(5339));

    /// Normalbelopp för ett barn utifrån ålder.
    public BigDecimal forBarn(int alder) {
        if (alder < 0) throw new IllegalArgumentException("ålder kan inte vara negativ: " + alder);
        if (alder <= 6) return barn0till6;
        if (alder <= 10) return barn7till10;
        if (alder <= 14) return barn11till14;
        return barn15ochAldre;
    }

    private static BigDecimal kr(int belopp) {
        return BigDecimal.valueOf(belopp);
    }
}
