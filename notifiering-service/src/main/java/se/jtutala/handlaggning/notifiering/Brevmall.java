package se.jtutala.handlaggning.notifiering;

import java.util.Optional;

/// Ansvar: **brevets text**. Alla statusbyten leder inte till ett brev.
final class Brevmall {

    private Brevmall() {
    }

    static Optional<String> textFor(ArendeStatusAndrad handelse) {
        return mallFor(handelse.till()).map(mall -> mall.formatted(handelse.arendenummer()));
    }

    /// Mall med `%s` där ärendenumret ska stå, eller tom om inget brev ska skickas.
    /// Okända statusar (t.ex. nya som ärendetjänsten lagt till) ger inget brev.
    private static Optional<String> mallFor(String status) {
        return Optional.ofNullable(switch (status) {
            case "DELGIVET" -> "Du har fått ett betalningsföreläggande (%s). Du har 10 dagar på dig att bestrida kravet om du inte håller med.";
            case "UTSLAG_MEDDELAT" -> "Ett utslag har meddelats i ärende %s. Skulden är fastställd och kan drivas in.";
            case "LONEUTMATNING" -> "Beslut om löneutmätning i ärende %s. Din arbetsgivare kommer att dra ett belopp från din lön.";
            case "BETALT" -> "Skulden i ärende %s är betald. Ärendet är avslutat.";
            case "SKULDSANERING_PAGAR" -> "Du har beviljats skuldsanering (%s). Följ din betalningsplan.";
            case "SKULDSANERING_AVSLAG" -> "Din ansökan om skuldsanering (%s) har avslagits.";
            case "SKULDSANERING_SLUTFORD" -> "Din skuldsanering (%s) är slutförd. Skulderna som ingick är borta.";
            default -> null;
        });
    }
}
