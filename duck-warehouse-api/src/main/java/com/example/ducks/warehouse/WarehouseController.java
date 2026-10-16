package com.example.ducks.warehouse;

import com.example.ducks.api.*;
import com.example.ducks.domain.Duck;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/ducks")
public class WarehouseController {
    private final WarehouseService service;
    public WarehouseController(WarehouseService service) { this.service = service; }
    @GetMapping public List<Duck> list() { return service.list(); }
    @GetMapping("/{id}") public Duck get(@PathVariable @Min(1) int id) { return service.get(id); }
    // Always 200: POST is an add-or-merge operation, not an unconditional create.
    @PostMapping public Duck add(@Valid @RequestBody AddDuckRequest request) { return service.add(request); }
    @PutMapping("/{id}") public Duck edit(@PathVariable @Min(1) int id, @Valid @RequestBody EditDuckRequest request) {
        return service.edit(id, request);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Min(1) int id) { service.delete(id); }
}
