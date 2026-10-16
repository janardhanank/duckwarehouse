package com.example.ducks.store;

import com.example.ducks.api.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.*;

class CountryPolicyTest {
    @ParameterizedTest
    @CsvSource({"USA,US,18", " us ,US,18", "United States,US,18", "Bolivia,BO,13", "India,IN,19", "gb,GB,15"})
    void acceptsDocumentedAliasesAndCountryCodes(String value, String expected, int rate) {
        assertThat(CountryPolicy.normalize(value)).isEqualTo(expected);
        assertThat(CountryPolicy.percentage(expected)).isEqualTo(rate);
    }
    @Test void rejectsUnknownCountries() {
        assertThatThrownBy(() -> CountryPolicy.normalize("ZZ")).isInstanceOf(ApiException.class);
    }
}
