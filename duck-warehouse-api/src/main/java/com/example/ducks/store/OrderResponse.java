package com.example.ducks.store;

import java.math.BigDecimal;
import java.util.List;

public record OrderResponse(String currency, int warehouseDuckId, BigDecimal unitPrice, int quantity,
    String destinationCountry, PackageType packageType, List<String> protectionTypes,
    BigDecimal totalToPay, List<PriceLine> breakdown) {}
