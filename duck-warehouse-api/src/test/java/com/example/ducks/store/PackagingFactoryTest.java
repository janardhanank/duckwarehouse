package com.example.ducks.store;

import com.example.ducks.domain.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.*;

class PackagingFactoryTest {
    private final PackagingFactory factory = new PackagingFactory();
    @ParameterizedTest
    @CsvSource({"XLarge,Wood", "Large,Wood", "Medium,Cardboard", "Small,Plastic", "XSmall,Plastic"})
    void everySizeAndShippingCombination(Size size, PackageType expected) {
        for (ShippingMode mode : ShippingMode.values()) {
            var result = factory.create(size, mode);
            assertThat(result.packageType()).isEqualTo(expected);
            switch (mode) {
                case Land -> assertThat(result.protectionTypes()).containsExactly("polystyrene balls");
                case Air -> assertThat(result.protectionTypes()).containsExactly(expected == PackageType.Plastic
                    ? "bubble-wrap bags" : "polystyrene balls");
                case Sea -> assertThat(result.protectionTypes()).containsExactly("moisture-absorbing beads", "bubble-wrap bags");
            }
        }
    }
}
