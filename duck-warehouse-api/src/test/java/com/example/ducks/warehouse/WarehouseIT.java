package com.example.ducks.warehouse;

import com.example.ducks.api.*;
import com.example.ducks.domain.*;
import com.example.ducks.store.StoreService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "RUN_DB_TESTS", matches = "true")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${TEST_DB_DATABASE:duck_warehouse_test}?connectionTimeZone=UTC"
})
class WarehouseIT {
    @Autowired WarehouseService warehouse;
    @Autowired StoreService store;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach void clearOnlyDedicatedTestDatabase() {
        String database = jdbc.queryForObject("SELECT DATABASE()", String.class);
        if (database == null || !database.endsWith("_test")) {
            throw new IllegalStateException("Integration tests require a database ending in _test");
        }
        jdbc.update("DELETE FROM ducks");
    }
    private AddDuckRequest add(String price, int quantity) {
        return new AddDuckRequest(Color.Red, Size.Large, new BigDecimal(price), quantity);
    }
    @Test void concurrentAddsMergeWithoutLostQuantities() throws Exception {
        var original = warehouse.add(add("10.00", 7));
        int writers = 20;
        ExecutorService executor = Executors.newFixedThreadPool(writers);
        CountDownLatch start = new CountDownLatch(1);
        var results = new ArrayList<Future<?>>();
        try {
            for (int i = 0; i < writers; i++) {
                results.add(executor.submit(() -> {
                    start.await();
                    return warehouse.add(add("10.00", 3));
                }));
            }
            start.countDown();
            for (Future<?> result : results) result.get(30, TimeUnit.SECONDS);
            assertThat(warehouse.list()).hasSize(1);
            assertThat(warehouse.get(original.id()).quantity()).isEqualTo(67);
        } finally { executor.shutdownNow(); }
    }
    @Test void concurrentFirstAddsAlsoCreateOnlyOneRecord() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch start = new CountDownLatch(1);
        var results = new ArrayList<Future<?>>();
        try {
            for (int i = 0; i < 10; i++) {
                results.add(executor.submit(() -> { start.await(); return warehouse.add(add("10.00", 2)); }));
            }
            start.countDown();
            for (Future<?> result : results) result.get(30, TimeUnit.SECONDS);
            assertThat(warehouse.list()).hasSize(1);
            assertThat(warehouse.list().get(0).quantity()).isEqualTo(20);
        } finally { executor.shutdownNow(); }
    }
    @Test void logicalDeletionKeepsHistoryAndAllowsNewActiveRecord() {
        var first = warehouse.add(add("10.00", 4));
        warehouse.delete(first.id());
        assertThat(warehouse.list()).isEmpty();
        assertThat(jdbc.queryForObject("SELECT deleted FROM ducks WHERE id=?", Boolean.class, first.id())).isTrue();
        var second = warehouse.add(add("10.00", 2));
        assertThat(second.id()).isNotEqualTo(first.id());
        warehouse.delete(second.id());
        warehouse.add(add("10.00", 1));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ducks", Integer.class)).isEqualTo(3);
        assertThatThrownBy(() -> warehouse.get(first.id())).isInstanceOf(ApiException.class);
    }
    @Test void editCollisionAndMergeOverflowRollback() {
        var first = warehouse.add(add("10.00", 3));
        var second = warehouse.add(add("20.00", 5));
        assertThatThrownBy(() -> warehouse.edit(second.id(), new EditDuckRequest(new BigDecimal("10.00"), 7)))
            .isInstanceOf(DuplicateKeyException.class);
        assertThat(warehouse.get(second.id()).price()).isEqualByComparingTo("20.00");
        assertThat(warehouse.get(second.id()).quantity()).isEqualTo(5);
        warehouse.edit(first.id(), new EditDuckRequest(new BigDecimal("10.00"), Integer.MAX_VALUE));
        assertThatThrownBy(() -> warehouse.add(add("10.00", 1))).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(warehouse.get(first.id()).quantity()).isEqualTo(Integer.MAX_VALUE);
    }
    @Test void listingUsesAscendingQuantityWithStableTieBreak() {
        var first = warehouse.add(add("10.00", 5));
        var second = warehouse.add(add("20.00", 2));
        var third = warehouse.add(add("30.00", 2));
        assertThat(warehouse.list()).extracting(Duck::id).containsExactly(second.id(), third.id(), first.id());
        warehouse.edit(first.id(), new EditDuckRequest(new BigDecimal("10.00"), 5));
        assertThat(warehouse.get(first.id()).quantity()).isEqualTo(5);
    }
    @Test void quoteChoosesCheapestSufficientLotWithoutConsumingStock() {
        var cheap = warehouse.add(add("1.00", 2));
        var sufficient = warehouse.add(add("10.00", 1000));
        warehouse.add(add("20.00", 1000));
        var request = new OrderRequest(Color.Red, Size.Large, 100, "USA", ShippingMode.Land);
        var quote = store.quote(request);
        assertThat(quote.warehouseDuckId()).isEqualTo(sufficient.id());
        assertThat(quote.totalToPay()).isEqualByComparingTo("2230.00");
        assertThat(warehouse.get(sufficient.id()).quantity()).isEqualTo(1000);
        assertThat(warehouse.get(cheap.id()).quantity()).isEqualTo(2);
        assertThatThrownBy(() -> store.quote(new OrderRequest(Color.Red, Size.Large, 1001, "US", ShippingMode.Sea)))
            .isInstanceOf(ApiException.class).hasMessageContaining("single");
        assertThatThrownBy(() -> store.quote(new OrderRequest(Color.Black, Size.XSmall, 1, "US", ShippingMode.Sea)))
            .isInstanceOf(ApiException.class).hasMessageContaining("No active");
    }
}
