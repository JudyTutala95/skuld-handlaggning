package se.jtutala.handlaggning.notifiering;

/// Händelsen från ärendetjänsten, så som **den här tjänsten** läser den.
///
/// Det är en egen kopia, inte ärendetjänstens klass. Tjänsterna delar bara JSON-formatet.
/// Status och typ är text i stället för enum (**tolerant läsare**): lägger ärendetjänsten
/// till en ny status går den här tjänsten inte sönder – den skickar bara inget brev.
record ArendeStatusAndrad(
        long arendeId,
        String arendenummer,
        long galdenarId,
        String typ,
        String fran,
        String till) {
}
