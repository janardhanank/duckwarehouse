CREATE TABLE ducks (
    id INT NOT NULL AUTO_INCREMENT,
    color VARCHAR(6) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NOT NULL,
    size VARCHAR(6) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NOT NULL,
    price DECIMAL(12,2) NOT NULL,
    quantity INT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    active_marker TINYINT GENERATED ALWAYS AS (
        CASE WHEN deleted = FALSE THEN 1 ELSE NULL END
    ) STORED,
    PRIMARY KEY (id),
    CONSTRAINT chk_ducks_color CHECK (color IN ('Red','Green','Yellow','Black')),
    CONSTRAINT chk_ducks_size CHECK (size IN ('XLarge','Large','Medium','Small','XSmall')),
    CONSTRAINT chk_ducks_price CHECK (price > 0),
    CONSTRAINT chk_ducks_quantity CHECK (quantity >= 0),
    CONSTRAINT chk_ducks_deleted CHECK (deleted IN (0,1)),
    CONSTRAINT uq_ducks_active_variant UNIQUE (color,size,price,active_marker),
    INDEX ix_ducks_active_quantity (deleted,quantity,id)
) ENGINE=InnoDB;
