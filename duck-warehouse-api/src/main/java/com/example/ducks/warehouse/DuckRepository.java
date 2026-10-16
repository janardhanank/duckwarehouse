package com.example.ducks.warehouse;

import com.example.ducks.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public class DuckRepository {
    private final JdbcTemplate jdbc;
    private static final String COLUMNS = "id,color,size,price,quantity";
    private static final RowMapper<Duck> MAPPER = (rs, row) -> new Duck(
        rs.getInt("id"), Color.valueOf(rs.getString("color")), Size.valueOf(rs.getString("size")),
        rs.getBigDecimal("price"), rs.getInt("quantity"));

    public DuckRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Duck> list() {
        return jdbc.query("SELECT " + COLUMNS + " FROM ducks WHERE deleted=FALSE ORDER BY quantity,id", MAPPER);
    }
    public Optional<Duck> find(int id, boolean lock) {
        return jdbc.query("SELECT " + COLUMNS + " FROM ducks WHERE id=? AND deleted=FALSE" +
            (lock ? " FOR UPDATE" : ""), MAPPER, id).stream().findFirst();
    }
    public Duck add(Color color, Size size, BigDecimal price, int quantity) {
        // Unique active variant + a single atomic increment; no read-before-write race.
        jdbc.update("""
            INSERT INTO ducks (color,size,price,quantity)
            VALUES (?,?,?,?) AS incoming
            ON DUPLICATE KEY UPDATE quantity=ducks.quantity+incoming.quantity
            """, color.name(), size.name(), price, quantity);
        return jdbc.queryForObject("SELECT " + COLUMNS +
            " FROM ducks WHERE color=? AND size=? AND price=? AND deleted=FALSE FOR UPDATE",
            MAPPER, color.name(), size.name(), price);
    }
    public void edit(int id, BigDecimal price, int quantity) {
        jdbc.update("UPDATE ducks SET price=?,quantity=? WHERE id=? AND deleted=FALSE", price, quantity, id);
    }
    public void delete(int id) {
        jdbc.update("UPDATE ducks SET deleted=TRUE WHERE id=? AND deleted=FALSE", id);
    }
    public Optional<Duck> cheapestAvailable(Color color, Size size, int quantity) {
        return jdbc.query("SELECT " + COLUMNS + """
             FROM ducks WHERE color=? AND size=? AND deleted=FALSE AND quantity>=?
             ORDER BY price,id LIMIT 1
            """, MAPPER, color.name(), size.name(), quantity).stream().findFirst();
    }
    public boolean hasVariant(Color color, Size size) {
        return !jdbc.queryForList("SELECT id FROM ducks WHERE color=? AND size=? AND deleted=FALSE LIMIT 1",
            Integer.class, color.name(), size.name()).isEmpty();
    }
}
