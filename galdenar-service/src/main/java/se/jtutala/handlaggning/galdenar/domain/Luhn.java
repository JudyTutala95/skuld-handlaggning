package se.jtutala.handlaggning.galdenar.domain;

/// Luhn-algoritmen (modulus 10), som används för kontrollsiffran i personnummer.
///
/// Varannan siffra från vänster (med början på den första) dubbleras. Blir produkten
/// tvåsiffrig dras 9 av. Summan av alla siffror ska vara delbar med 10.
final class Luhn {

    private Luhn() {
    }

    static boolean arGiltigt(String siffror) {
        return summa(siffror) % 10 == 0;
    }

    private static int summa(String siffror) {
        int summa = 0;
        for (int i = 0; i < siffror.length(); i++) {
            summa += vikt(siffror.charAt(i) - '0', i);
        }
        return summa;
    }

    private static int vikt(int siffra, int position) {
        int produkt = position % 2 == 0 ? siffra * 2 : siffra;
        return produkt > 9 ? produkt - 9 : produkt;
    }
}
