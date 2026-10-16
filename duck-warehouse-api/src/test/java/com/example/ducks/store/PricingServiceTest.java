package com.example.ducks.store;

import com.example.ducks.domain.ShippingMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;

class PricingServiceTest {
    private final PricingService pricing = new PricingService();

    @ParameterizedTest
    @CsvSource({
        "100,10.00,Wood,US,Land,2230.00",
        "101,10.00,Wood,US,Land,2050.30",
        "1000,1.00,Plastic,IN,Air,31090.00",
        "1001,1.00,Plastic,IN,Air,26616.59",
        "100,2.00,Cardboard,BO,Sea,624.00",
        "1,0.05,Plastic,GB,Land,10.07"
    })
    void calculatesThresholdsAndRoundsEachLine(int quantity, BigDecimal unitPrice, PackageType type,
        String country, ShippingMode mode, BigDecimal expected) {
        var context = new PricingContext(unitPrice.multiply(BigDecimal.valueOf(quantity)), quantity, type, country, mode);
        var result = pricing.calculate(context);
        assertThat(result.totalToPay()).isEqualByComparingTo(expected);
        assertThat(result.totalToPay()).isEqualByComparingTo(result.breakdown().stream()
            .map(PriceLine::amount).reduce(BigDecimal.ZERO, BigDecimal::add));
        assertThat(result.breakdown()).allSatisfy(line -> assertThat(line.amount().scale()).isEqualTo(2));
    }
    @Test
    void discountsOnlyAirChargeAtTheAirThreshold() {
        var result = pricing.calculate(new PricingContext(new BigDecimal("100100.00"), 1001,
            PackageType.Wood, "US", ShippingMode.Air));
        var discount = result.breakdown().stream().filter(l -> l.code().equals("AIR_DISCOUNT")).findFirst().orElseThrow();
        assertThat(discount.basis()).isEqualByComparingTo("30030.00");
        assertThat(discount.amount()).isEqualByComparingTo("-4504.50");
    }
    @Test
    void handlesAmountsLargerThanIntegerAndFloatingPointRangesExactly() {
        var subtotal = new BigDecimal("9999999999.99").multiply(BigDecimal.valueOf(Integer.MAX_VALUE));
        var result = pricing.calculate(new PricingContext(subtotal, Integer.MAX_VALUE, PackageType.Plastic, "IN", ShippingMode.Sea));
        // Independently calculated with each adjustment rounded separately.
        assertThat(result.totalToPay()).isEqualByComparingTo("23407571752276592828.24");
    }
}
