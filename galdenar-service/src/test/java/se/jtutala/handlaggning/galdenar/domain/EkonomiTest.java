package se.jtutala.handlaggning.galdenar.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class EkonomiTest {

    @Test
    void barnensAlderRaknasUtFranFodelsear() {
        Ekonomi ekonomi = Ekonomi.av(BigDecimal.TEN, BigDecimal.ONE, true, List.of(2018, 2012), 2026);

        var hushall = ekonomi.somHushall(LocalDate.of(2026, 6, 1));

        assertThat(hushall.barnensAldrar()).containsExactly(8, 14);
        assertThat(hushall.sammanlevande()).isTrue();
    }

    @Test
    void barnFoddIFramtidenAvvisas() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Ekonomi.av(BigDecimal.TEN, BigDecimal.ONE, false, List.of(2027), 2026));
    }

    @Test
    void forGamlaBarnAvvisas() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Ekonomi.av(BigDecimal.TEN, BigDecimal.ONE, false, List.of(1990), 2026));
    }
}
