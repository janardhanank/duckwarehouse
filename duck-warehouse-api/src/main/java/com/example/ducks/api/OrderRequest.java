package com.example.ducks.api;

import com.example.ducks.domain.Color;
import com.example.ducks.domain.Size;
import com.example.ducks.domain.ShippingMode;
import jakarta.validation.constraints.*;

public record OrderRequest(
    @NotNull Color color,
    @NotNull Size size,
    @NotNull @Min(1) Integer quantity,
    @NotBlank @jakarta.validation.constraints.Size(max = 80) String destinationCountry,
    @NotNull ShippingMode shippingMode
) {}
