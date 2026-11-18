package se.jtutala.handlaggning.notifiering;

/// Ansvar: **leverera** ett brev till en gäldenär. Vad brevet innehåller avgör `Brevmall`.
///
/// Ett interface gör att leveranssättet (logg, PDF, digital brevlåda) kan bytas utan att
/// resten av koden ändras.
interface Brevlada {

    void skicka(long galdenarId, String text);
}
