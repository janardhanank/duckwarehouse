package com.example.ducks.store;

import com.example.ducks.api.OrderRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/store/orders")
public class StoreController {
    private final StoreService service;
    public StoreController(StoreService service) { this.service = service; }
    @PostMapping public OrderResponse quote(@Valid @RequestBody OrderRequest request) {
        return service.quote(request);
    }
}
