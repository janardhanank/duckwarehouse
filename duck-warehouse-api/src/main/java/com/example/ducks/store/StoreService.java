package com.example.ducks.store;

import com.example.ducks.api.*;
import com.example.ducks.domain.Duck;
import com.example.ducks.warehouse.DuckRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class StoreService {
    private final DuckRepository repository;
    private final PackagingFactory packaging;
    private final PricingService pricing;
    public StoreService(DuckRepository repository, PackagingFactory packaging, PricingService pricing) {
        this.repository = repository;
        this.packaging = packaging;
        this.pricing = pricing;
    }
    public OrderResponse quote(OrderRequest request) {
        String country = CountryPolicy.normalize(request.destinationCountry());
        Duck duck = repository.cheapestAvailable(request.color(), request.size(), request.quantity())
            .orElseThrow(() -> unavailable(request));
        Packaging pack = packaging.create(request.size(), request.shippingMode());
        BigDecimal subtotal = duck.price().multiply(BigDecimal.valueOf(request.quantity())).setScale(2);
        PriceCalculation calculated = pricing.calculate(new PricingContext(subtotal, request.quantity(),
            pack.packageType(), country, request.shippingMode()));
        return new OrderResponse("USD", duck.id(), duck.price(), request.quantity(), country,
            pack.packageType(), pack.protectionTypes(), calculated.totalToPay(), calculated.breakdown());
    }
    private ApiException unavailable(OrderRequest request) {
        if (!repository.hasVariant(request.color(), request.size())) {
            return new ApiException(HttpStatus.NOT_FOUND, "DUCK_NOT_FOUND", "No active duck matches this color and size");
        }
        return new ApiException(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK",
            "No single matching warehouse record can supply the full quantity");
    }
}
