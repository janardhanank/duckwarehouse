package com.example.ducks.store;

import java.util.List;

public record Packaging(PackageType packageType, List<String> protectionTypes) {
    public Packaging { protectionTypes = List.copyOf(protectionTypes); }
}
