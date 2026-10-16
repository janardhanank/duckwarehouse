package com.example.ducks.store;

import java.math.BigDecimal;
import java.util.List;

public record PriceCalculation(BigDecimal subtotal, BigDecimal totalToPay, List<PriceLine> breakdown) {
    public PriceCalculation { breakdown = List.copyOf(breakdown); }
}
