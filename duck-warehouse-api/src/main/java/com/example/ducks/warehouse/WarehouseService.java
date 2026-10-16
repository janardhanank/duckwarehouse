package com.example.ducks.warehouse;

import com.example.ducks.api.*;
import com.example.ducks.domain.Duck;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.math.RoundingMode;
import java.util.List;

@Service
public class WarehouseService {
    private final DuckRepository repository;
    private final WriteTransactions transactions;
    public WarehouseService(DuckRepository repository, WriteTransactions transactions) {
        this.repository = repository;
        this.transactions = transactions;
    }
    public List<Duck> list() { return repository.list(); }
    public Duck get(int id) { return repository.find(id, false).orElseThrow(this::notFound); }
    public Duck add(AddDuckRequest request) {
        return transactions.addWithDeadlockRetry(() -> repository.add(request.color(), request.size(),
            request.price().setScale(2, RoundingMode.UNNECESSARY), request.quantity()));
    }
    public Duck edit(int id, EditDuckRequest request) {
        return transactions.run(() -> {
            Duck existing = repository.find(id, true).orElseThrow(this::notFound);
            var price = request.price().setScale(2, RoundingMode.UNNECESSARY);
            repository.edit(id, price, request.quantity());
            return new Duck(id, existing.color(), existing.size(), price, request.quantity());
        });
    }
    public void delete(int id) {
        transactions.run(() -> {
            repository.find(id, true).orElseThrow(this::notFound);
            repository.delete(id);
            return null;
        });
    }
    private ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "DUCK_NOT_FOUND", "Active duck not found");
    }
}
