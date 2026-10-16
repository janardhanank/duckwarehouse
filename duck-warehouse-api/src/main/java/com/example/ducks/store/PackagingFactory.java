package com.example.ducks.store;

import com.example.ducks.domain.Size;
import com.example.ducks.domain.ShippingMode;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class PackagingFactory {
    private interface ProtectionStrategy { List<String> forPackage(PackageType type); }
    private final Map<ShippingMode, ProtectionStrategy> protections = Map.of(
        ShippingMode.Land, type -> List.of("polystyrene balls"),
        ShippingMode.Air, type -> type == PackageType.Plastic
            ? List.of("bubble-wrap bags") : List.of("polystyrene balls"),
        ShippingMode.Sea, type -> List.of("moisture-absorbing beads", "bubble-wrap bags")
    );
    public Packaging create(Size size, ShippingMode shippingMode) {
        PackageType type = switch (size) {
            case XLarge, Large -> PackageType.Wood;
            case Medium -> PackageType.Cardboard;
            case Small, XSmall -> PackageType.Plastic;
        };
        return new Packaging(type, protections.get(shippingMode).forPackage(type));
    }
}
