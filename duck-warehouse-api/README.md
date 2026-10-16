# Duck Warehouse — Spring Boot API

Complete backend for the warehouse and store exercise. Java 17, Spring Boot
3.5.16, Maven, MySQL 8.4, Spring JDBC, Jakarta Validation, and Flyway. No frontend
or persisted order module is included: this deliverable is the requested API.

## Run on Windows

Prerequisites: a full JDK 17 or newer, Maven 3.6.3 or newer, and MySQL 8.4
(either installed locally or supplied by Docker Desktop). Check `java -version`
and `mvn -version`. The full JDK is required to compile.

Extract this folder to `C:\Users\JANARDHANA.NK\Downloads\log\duck-warehouse-api`.

### Option A: MySQL through Docker Desktop

Open PowerShell in the project folder:

```powershell
Copy-Item .env.example .env
notepad .env
# Replace the two example passwords, then save and close Notepad.
docker compose up -d --wait
```

Load the same `.env` values into the current PowerShell process:

```powershell
Get-Content .env | ForEach-Object {
    if ($_ -match '^([A-Z_]+)=(.*)$') {
        [Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process')
    }
}
mvn spring-boot:run
```

Compose reads `.env`; Spring Boot does not automatically read that file. The
PowerShell command above passes its values to Spring Boot. Do not commit `.env`.
Changing an existing MySQL volume's credentials requires database user changes;
changing `.env` alone does not change users in an already initialized volume.

### Option B: Existing local MySQL installation

Run `database/create_database.sql` through MySQL Workbench or the MySQL CLI.
Create a dedicated application user with privileges on `duck_warehouse`.
For this exercise Flyway needs table/index creation and schema-history write
privileges as well as application SELECT, INSERT, and UPDATE privileges.

```powershell
$env:DB_HOST = 'localhost'
$env:DB_PORT = '3306'
$env:DB_DATABASE = 'duck_warehouse'
$env:DB_USERNAME = 'your_local_database_user'
$env:DB_PASSWORD = 'your_local_database_password'
mvn spring-boot:run
```

Use a fresh database. Flyway creates `ducks` and `flyway_schema_history` on the
first startup. If you already ran the standalone schema supplied earlier, use
a new empty database for this project; do not silently baseline an unverified
existing table. The application fails startup if migrations fail.

Base URL: `http://localhost:8080`. Override the port with `SERVER_PORT`.

To build and run a standalone JAR:

```powershell
mvn clean package
java -jar target/duck-warehouse-api-1.0.0.jar
```

Linux/macOS users can set the same variables with `export`, then use the same
Maven commands. Stop Docker MySQL with `docker compose down`; its named volume
retains the data.

## API contract

All request bodies are JSON. Color, size, and shipping labels are case-sensitive.
Responses use lower camelCase. Prices are JSON decimal numbers with two decimal
places; clients should use decimal arithmetic when processing money.

| Method | Path | Success | Behavior |
|---|---|---|---|
| GET | `/api/ducks` | 200 | Active records sorted by quantity ascending, then ID |
| GET | `/api/ducks/{id}` | 200 | Get an active record for editing |
| POST | `/api/ducks` | 200 | Create or atomically merge quantity; return resulting record |
| PUT | `/api/ducks/{id}` | 200 | Replace price and quantity; color and size cannot be changed |
| DELETE | `/api/ducks/{id}` | 204 | Logically delete an active record |
| POST | `/api/store/orders` | 200 | Calculate an order quote from current warehouse data |

`POST /api/ducks` returns 200 for both creation and merging: its contract is an
add-or-merge operation. No client-supplied identifiers are accepted.

### Add a duck

```json
{
  "color": "Red",
  "size": "Large",
  "price": 10.00,
  "quantity": 1000
}
```

Example response:

```json
{"id":1,"color":"Red","size":"Large","price":10.00,"quantity":1000}
```

Adding the same color, size, and price again increases the existing quantity.
Price may also be sent as an exact decimal string, such as `"10.00"`.

### Edit a duck

`PUT /api/ducks/1`

```json
{"price":12.50,"quantity":900}
```

Both fields are required. Quantity is an absolute replacement. A price change
that creates a duplicate active variant returns 409 and rolls back the edit.
An unchanged edit succeeds. Sending color or size returns 400; the frontend may
display those values read-only but must omit them from the edit payload.

### Store quote

`POST /api/store/orders`

```json
{
  "color": "Red",
  "size": "Large",
  "quantity": 100,
  "destinationCountry": "USA",
  "shippingMode": "Land"
}
```

For a matching warehouse unit price of $10.00:

```json
{
  "currency": "USD",
  "warehouseDuckId": 1,
  "unitPrice": 10.00,
  "quantity": 100,
  "destinationCountry": "US",
  "packageType": "Wood",
  "protectionTypes": ["polystyrene balls"],
  "totalToPay": 2230.00,
  "breakdown": [
    {"code":"DUCK_SUBTOTAL","description":"Quantity multiplied by warehouse unit price","basis":1000.00,"ratePercent":null,"amount":1000.00},
    {"code":"WOOD_SURCHARGE","description":"Wood packaging","basis":1000.00,"ratePercent":5,"amount":50.00},
    {"code":"DESTINATION_SURCHARGE","description":"Destination US","basis":1000.00,"ratePercent":18,"amount":180.00},
    {"code":"SHIPPING_CHARGE","description":"Land shipment","basis":1000.00,"ratePercent":null,"amount":1000.00}
  ]
}
```

Destination accepts ISO 3166-1 alpha-2 codes, case-insensitively, plus `USA`,
`United States`, `United States of America`, `Bolivia`, and `India`. Use `GB`
for the UK and `DE` for Germany. Invalid codes are rejected, and responses
always return the normalized two-letter code. The supported ISO code set is
explicit in `CountryPolicy` and must be updated when ISO changes it.

### Errors

```json
{"code":"VALIDATION_ERROR","message":"Invalid request fields","fields":{"quantity":"must be greater than or equal to 1"}}
```

| Status | Meaning |
|---|---|
| 400 | Invalid fields, JSON, enum, country, identifier, unknown field, or duplicate JSON key |
| 404 | Active duck missing, deleted, or no matching color/size for an order |
| 409 | Edit collision, merged quantity overflow, or insufficient stock in a single lot |
| 415 | Request body is not `application/json` |
| 503 | Database unavailable or exhausted transient transaction retries |
| 500 | Unexpected internal error; details stay in server logs |

Quantity must be a JSON integer, not a float or string. Its maximum is
2,147,483,647. Add and order quantities are positive; an edit may set stock to
zero. Unit price is $0.01 through $9,999,999,999.99, with at most two decimal
places. Missing/null values, unsupported labels, and extra fields are rejected.

## Deliberate business decisions

1. **Money:** `DECIMAL(12,2)` in MySQL and `BigDecimal` in Java replace binary
   floating-point storage. Every percentage adjustment is rounded separately
   to cents using `HALF_UP`. Total is the exact sum of displayed line amounts.
2. **Percentage basis:** bulk, package, and destination percentages all use the
   original `quantity × unitPrice` subtotal. They do not compound. The air
   discount uses only the air shipping charge. This is a deliberate reading of
   each requirement's reference to “the total cost.”
3. **Thresholds:** bulk discount applies at 101 units, air discount at 1001.
4. **Pricing source:** choose the cheapest active matching lot with enough
   units to fulfill the entire requested quantity. Tie-break by ID. Do not
   combine differently priced lots. If cheaper lots lack enough stock, choose
   the cheapest sufficiently stocked lot; if none qualifies, return 409.
5. **Quote semantics:** the one store endpoint calculates a quote. It does not
   reserve stock, decrement quantity, persist an order, or process payment.
   The requirements ask for calculation and do not define checkout semantics.
   Availability is a snapshot and may change after the quote is returned.
6. **Deletion:** deleted records remain in the database; adding that variant
   later creates a new active record. Historical IDs remain unchanged.
7. **Edit collisions:** reject with 409 rather than merging during edits.
8. **Listing:** lowest quantity first, then lowest ID for deterministic ties.
9. **Concurrent edits:** warehouse writes use row locks. Absolute quantity
   edits use last-write-wins behavior; they are replacements, not increments.
10. **Delete confirmation:** belongs in the eventual frontend; the backend
    executes the confirmed DELETE request and returns 404 for an already
    deleted record. There is no physical-delete endpoint.

Worked threshold examples:

| Quantity | Unit price | Package | Destination | Mode | Total |
|---|---|---|---|---|---|
| 100 | 10.00 | Wood | US | Land | 2230.00 |
| 101 | 10.00 | Wood | US | Land | 2050.30 |
| 1000 | 1.00 | Plastic | IN | Air | 31090.00 |
| 1001 | 1.00 | Plastic | IN | Air | 26616.59 |
| 100 | 2.00 | Cardboard | BO | Sea | 624.00 |

The 1001-unit air example is
`1001.00 - 200.20 + 100.10 + 190.19 + 30030.00 - 4504.50 = 26616.59`.

## Concurrency and database design

`ducks` has the six required business fields plus an internal generated
`active_marker`. Active rows have marker 1; deleted rows have NULL. The unique
index `(color,size,price,active_marker)` permits only one matching active row,
while allowing multiple deleted historical rows. MySQL unique indexes permit
multiple NULL entries.

Adding uses one `INSERT ... ON DUPLICATE KEY UPDATE quantity = quantity + incoming.quantity`
inside an InnoDB transaction. It does not perform a separate existence check.
The returned row is read while the transaction still holds its write lock.
Edits and deletions lock the target active row. Quantity overflow rejects the
operation without modifying the record. IDs are stable but may have gaps.

Only MySQL deadlock error 1213 is retried, at most three transaction attempts;
that error guarantees the transaction was rolled back. Connection failures and
unknown commit outcomes are not retried. POST adds are not idempotent: after
an ambiguous network failure, inspect the warehouse before resubmitting.

`ix_ducks_active_quantity` supports active filtering and quantity ordering.
The unique variant index also supports color/size lookups for quote selection.

## Structure and patterns

```text
src/main/java/com/example/ducks/
  api/          Request DTOs, validation, and consistent errors
  config/       Strict JSON handling
  domain/       Warehouse record and enums
  warehouse/    Controller, service, JDBC repository, transaction retry policy
  store/        Controller, orchestration, packaging factory, pricing pipeline
src/main/resources/db/migration/  Versioned Flyway schema
src/test/java/                   Unit, HTTP validation, and MySQL integration tests
```

Controllers adapt HTTP; services express business operations; the repository
owns SQL. `PackagingFactory` selects package material and delegates protection
selection to shipping strategies. Pricing uses a composable `PricingRule`
pipeline: bulk, package, destination, and shipping rules contribute signed
itemized lines. No business calculation resides in controllers or SQL.

## Automated tests

Fast tests, without a database:

```powershell
mvn test
```

They cover packaging combinations, country normalization, pricing thresholds,
rounding, large monetary values, strict request validation, and edit restrictions.

MySQL integration tests require a separate empty database named
`duck_warehouse_test`. Create it as an administrator and grant the application
user privileges on it. Do not reuse your application database. The tests delete
duck rows in the test database before each test. They refuse cleanup unless the
connected database name ends in `_test`.

```powershell
$env:RUN_DB_TESTS = 'true'
$env:TEST_DB_DATABASE = 'duck_warehouse_test'
# DB_HOST, DB_PORT, DB_USERNAME and DB_PASSWORD use your test database connection.
mvn verify
```

`WarehouseIT` covers concurrent first creation, concurrent merging, retained
deleted history, edit conflict rollback, overflow rollback, sorted listing,
price selection, and quotes that do not consume inventory. With
`RUN_DB_TESTS` unset these integration tests are explicitly skipped. `mvn test`
alone is not evidence that MySQL concurrency checks passed.

## Repository and scope

This is a backend source deliverable, ready to put in your own Git repository.
No fabricated development history is provided. Commit your actual changes as
you work, and keep credentials, build output, editor state, and chat logs out of
the repository. `.gitignore` covers common local files.

Authentication is outside the exercise's specification. Run this service locally
for evaluation. A frontend can use relative `/api` URLs through its development
proxy; no permissive CORS policy is enabled by default.

Official references:
- https://docs.spring.io/spring-boot/3.5/index.html
- https://dev.mysql.com/doc/refman/8.4/en/insert-on-duplicate.html
- https://dev.mysql.com/doc/refman/8.4/en/create-table-generated-columns.html
