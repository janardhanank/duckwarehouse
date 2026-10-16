package com.example.ducks.domain;

import java.math.BigDecimal;

public record Duck(int id, Color color, Size size, BigDecimal price, int quantity) {}
