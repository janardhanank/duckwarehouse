package com.example.ducks.store;

import java.math.BigDecimal;

// Positive amount = increment; negative amount = discount.
public record PriceLine(String code, String description, BigDecimal basis,
    BigDecimal ratePercent, BigDecimal amount) {}
