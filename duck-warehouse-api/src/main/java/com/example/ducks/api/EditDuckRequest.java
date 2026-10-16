package com.example.ducks.api;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record EditDuckRequest(
    @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal price,
    @NotNull @Min(0) Integer quantity
) {}
