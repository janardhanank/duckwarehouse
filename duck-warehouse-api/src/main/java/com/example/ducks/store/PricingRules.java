package com.example.ducks.store;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

final class PricingRules {
    private PricingRules() {}
    static PricingRule bulk() {
        return c -> c.quantity() > 100
            ? List.of(percent("BULK_DISCOUNT", "More than 100 units", c.subtotal(), -20)) : List.of();
    }
    static PricingRule packaging() {
        return c -> List.of(switch (c.packageType()) {
            case Wood -> percent("WOOD_SURCHARGE", "Wood packaging", c.subtotal(), 5);
            case Plastic -> percent("PLASTIC_SURCHARGE", "Plastic packaging", c.subtotal(), 10);
            case Cardboard -> percent("CARDBOARD_DISCOUNT", "Cardboard packaging", c.subtotal(), -1);
        });
    }
    static PricingRule destination() {
        return c -> List.of(percent("DESTINATION_SURCHARGE", "Destination " + c.destinationCountry(),
            c.subtotal(), CountryPolicy.percentage(c.destinationCountry())));
    }
    static PricingRule shipping() {
        return c -> {
            BigDecimal charge = switch (c.shippingMode()) {
                case Sea -> new BigDecimal("400.00");
                case Land -> BigDecimal.valueOf(c.quantity()).multiply(new BigDecimal("10.00"));
                case Air -> BigDecimal.valueOf(c.quantity()).multiply(new BigDecimal("30.00"));
            };
            List<PriceLine> lines = new ArrayList<>();
            lines.add(new PriceLine("SHIPPING_CHARGE", c.shippingMode() + " shipment", charge, null, charge));
            if (c.shippingMode() == com.example.ducks.domain.ShippingMode.Air && c.quantity() > 1000) {
                lines.add(percent("AIR_DISCOUNT", "More than 1000 units; discount on air charge", charge, -15));
            }
            return List.copyOf(lines);
        };
    }
    private static PriceLine percent(String code, String description, BigDecimal basis, int percentage) {
        // HALF_UP rounds positive and negative amounts symmetrically.
        BigDecimal rate = BigDecimal.valueOf(percentage);
        BigDecimal amount = basis.multiply(rate).movePointLeft(2).setScale(2, RoundingMode.HALF_UP);
        return new PriceLine(code, description, basis, rate, amount);
    }
}
