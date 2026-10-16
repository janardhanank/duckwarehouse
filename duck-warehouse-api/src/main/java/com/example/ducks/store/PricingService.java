package com.example.ducks.store;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class PricingService {
    // Ordered rule pipeline. Add one rule without changing the controller or repository.
    private final List<PricingRule> rules = List.of(PricingRules.bulk(), PricingRules.packaging(),
        PricingRules.destination(), PricingRules.shipping());
    public PriceCalculation calculate(PricingContext context) {
        var lines = new ArrayList<PriceLine>();
        lines.add(new PriceLine("DUCK_SUBTOTAL", "Quantity multiplied by warehouse unit price",
            context.subtotal(), null, context.subtotal()));
        rules.forEach(rule -> lines.addAll(rule.apply(context)));
        BigDecimal total = lines.stream().map(PriceLine::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PriceCalculation(context.subtotal(), total.setScale(2), lines);
    }
}
