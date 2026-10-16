package com.example.ducks.store;

import com.example.ducks.domain.ShippingMode;
import java.math.BigDecimal;

public record PricingContext(BigDecimal subtotal, int quantity, PackageType packageType,
    String destinationCountry, ShippingMode shippingMode) {}
