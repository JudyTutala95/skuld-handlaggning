package se.jtutala.handlaggning.galdenar;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LuhnTest {

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({"1212121212, true", "1212121213, false", "8503152384, true", "8503152385, false"})
    void kontrollerarKontrollsiffran(String siffror, boolean giltigt) {
        assertThat(Luhn.arGiltigt(siffror)).isEqualTo(giltigt);
    }
}
