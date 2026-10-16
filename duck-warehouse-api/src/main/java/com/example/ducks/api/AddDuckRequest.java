package com.example.ducks.api;

import com.example.ducks.domain.Color;
import com.example.ducks.domain.Size;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record AddDuckRequest(
    @NotNull Color color,
    @NotNull Size size,
    @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal price,
    @NotNull @Min(1) Integer quantity
) {}
