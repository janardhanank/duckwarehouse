-- Duck Warehouse: database schema for MySQL 8.4.
-- Run this file with MySQL Workbench or the mysql command-line client.
-- No records are inserted or deleted by this script.
-- Execute once on a new database; existing table conflicts fail visibly.

CREATE DATABASE IF NOT EXISTS duck_warehouse
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_bin;

USE duck_warehouse;

CREATE TABLE ducks (
    id INT NOT NULL AUTO_INCREMENT,
    color VARCHAR(6) NOT NULL,
    size VARCHAR(6) NOT NULL,
    price DECIMAL(12, 2) NOT NULL COMMENT 'Unit price in USD; exact cents',
    quantity INT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    -- MySQL UNIQUE indexes permit multiple NULL values.
    -- Active rows use 1; deleted rows use NULL, allowing historical duplicates.
    -- Never accept this generated field from an API request.
    active_marker TINYINT GENERATED ALWAYS AS (
        CASE WHEN deleted = FALSE THEN 1 ELSE NULL END
    ) STORED,

    PRIMARY KEY (id),
    CONSTRAINT chk_ducks_color
        CHECK (color IN ('Red', 'Green', 'Yellow', 'Black')),
    CONSTRAINT chk_ducks_size
        CHECK (size IN ('XLarge', 'Large', 'Medium', 'Small', 'XSmall')),
    CONSTRAINT chk_ducks_price CHECK (price > 0),
    CONSTRAINT chk_ducks_quantity CHECK (quantity >= 0),
    CONSTRAINT chk_ducks_deleted CHECK (deleted IN (0, 1)),

    CONSTRAINT uq_ducks_active_variant
        UNIQUE (color, size, price, active_marker),
    INDEX ix_ducks_active_quantity (deleted, quantity, id)
) ENGINE = InnoDB;

-- Design decisions / API responsibilities:
-- 1. DECIMAL replaces the requested Double to avoid binary floating-point
--    errors in USD calculations. The API rejects prices with >2 decimal places
--    or outside 0.01..9999999999.99 before sending them to MySQL.
-- 2. Quantity in stock may be zero; add quantities and order quantities must
--    be positive integers. Maximum quantity is 2147483647. Reject overflow,
--    including overflow from merging, without changing the existing row.
-- 3. List only deleted = FALSE, ordered by quantity ASC and then id ASC.
-- 4. A new add does not restore a deleted row: it creates/merges an active row.
-- 5. Editing a price into another active variant is rejected as HTTP 409.
--    The unique constraint enforces this; edits do not silently merge rows.
-- 6. The API exposes only price and quantity as editable fields, and never
--    accepts id, deleted, active_marker, color, or size in edit requests.
-- 7. IDs are stable identifiers, not gap-free sequence numbers.
-- 8. Strict SQL mode must remain enabled on all application connections.
--    Application validation must reject incorrect types, unknown fields,
--    invalid values, and trailing spaces instead of relying on coercion.
-- 9. The store calculation endpoint needs no additional tables: packaging,
--    pricing adjustments, and the response are computed in application code.
--    Price selection and whether ordering consumes stock must be documented
--    when the store API is implemented. Order persistence is not required.

-- Required atomic add statement (execute as a prepared statement in the API):
-- Do not implement add as a separate SELECT followed by INSERT/UPDATE.
-- The unique key and InnoDB make the increment atomic for concurrent adds.
-- Retry deadlocks using a bounded retry policy for rolled-back transactions.
-- Do not automatically retry an unknown commit outcome: that can double-add.
--
-- INSERT INTO ducks (color, size, price, quantity)
-- VALUES (?, ?, ?, ?) AS incoming
-- ON DUPLICATE KEY UPDATE
--     quantity = ducks.quantity + incoming.quantity,
--     id = LAST_INSERT_ID(ducks.id);
--
-- The driver's insertId identifies either the created row or the merged row.
-- If using SELECT LAST_INSERT_ID(), keep it on the same connection.

-- List:
-- SELECT id, color, size, price, quantity
-- FROM ducks
-- WHERE deleted = FALSE
-- ORDER BY quantity ASC, id ASC;

-- Edit (a duplicate-key error maps to HTTP 409):
-- UPDATE ducks SET price = ?, quantity = ?
-- WHERE id = ? AND deleted = FALSE;
-- An unchanged edit is still successful; do not confuse changed rows with
-- matched rows when choosing between success and HTTP 404.

-- Logical delete (UI must request confirmation before calling the API):
-- UPDATE ducks SET deleted = TRUE
-- WHERE id = ? AND deleted = FALSE;
