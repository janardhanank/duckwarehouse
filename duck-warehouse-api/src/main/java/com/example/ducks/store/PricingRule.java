package com.example.ducks.store;

import java.util.List;

@FunctionalInterface
public interface PricingRule {
    List<PriceLine> apply(PricingContext context);
}
