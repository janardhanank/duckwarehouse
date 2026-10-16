# Duck Warehouse - Setup and Run

Spring Boot API + React warehouse UI + MySQL database. The UI supports listing, adding, editing, and logically deleting stock. The store API calculates quotes; it does not reserve stock or save orders.

These instructions use **Windows PowerShell** and this project folder:

```text
C:$path
```

## 1. Project folders

```text
project/
  README.md
  duck-warehouse-springboot/
    duck-warehouse-api/        Backend: pom.xml, compose.yaml, src/
  duck-warehouse-react-ui/
    duck-warehouse-ui/         Frontend: package.json, vite.config.js, src/
  Sourcecode/                  Another copy of the backend and frontend
  Sourcecode.zip               Source archive
  Duck_Warehouse_10000_Concurrent_Requests_Guide.docx
```

Use the two folders under **duck-warehouse-springboot** and **duck-warehouse-react-ui** below. Make changes and run builds from the same copy.

## 2. Prerequisites

| Tool | Requirement |
| --- | --- |
| Java | Full JDK 17, matching the backend's Java target |
| Maven | Maven 3.6.3 or newer, available on PATH |
| Node.js | Version 22.12.0 or newer, required by this frontend |
| npm | Installed with Node.js |
| MySQL | MySQL 8.4, through Docker Compose or a local installation |
| Docker option | Docker Desktop running Linux containers, with Docker Compose v2 |
| Network | Maven Central, npm registry, and Docker registry access for first-time downloads |

Check your tools:

```powershell
java -version
javac -version
mvn.cmd -version
node --version
npm.cmd --version
docker --version
docker compose version
```

The Docker checks apply only if using Docker for MySQL. Ensure Maven reports JDK 17; correct JAVA_HOME and PATH if necessary. There is no Maven wrapper in this project.

The Windows launchers npm.cmd and mvn.cmd avoid ambiguity with PowerShell script launchers.

## 3. Start MySQL and the API with Docker

Compose starts **MySQL only**. Run Spring Boot separately.

### A. Create the backend environment file

Open PowerShell:

```powershell
Set-Location 'C:$path\duck-warehouse-springboot\duck-warehouse-api'

if (-not (Test-Path -LiteralPath '.env')) {
    Copy-Item -LiteralPath '.env.example' -Destination '.env'
}
notepad .env
```

Replace both example passwords, save, and close Notepad. The supplied template contains:

```dotenv
DB_DATABASE=duck_warehouse
DB_USERNAME=duck_app
DB_PASSWORD=replace-with-your-local-password
MYSQL_ROOT_PASSWORD=replace-with-your-local-root-password
DB_PORT=3306
```

Keep .env beside compose.yaml. Use different application and root passwords. Compose supports quoting and interpolation; see its [environment-file documentation](https://docs.docker.com/compose/how-tos/environment-variables/variable-interpolation/).

### B. Load connection settings and start MySQL

Run in the **same backend PowerShell window**:

```powershell
$composeJson = docker compose --env-file .env config --format json
if ($LASTEXITCODE -ne 0) {
    throw 'Check the backend .env file and Docker Compose configuration.'
}

$mysqlConfig = ($composeJson | ConvertFrom-Json).services.mysql
$env:DB_HOST = '127.0.0.1'
$env:DB_PORT = [string]$mysqlConfig.ports[0].published
$env:DB_DATABASE = [string]$mysqlConfig.environment.MYSQL_DATABASE
$env:DB_USERNAME = [string]$mysqlConfig.environment.MYSQL_USER
$env:DB_PASSWORD = [string]$mysqlConfig.environment.MYSQL_PASSWORD

docker compose --env-file .env up -d --wait
if ($LASTEXITCODE -ne 0) {
    throw 'MySQL did not become healthy. Check docker compose logs mysql.'
}
docker compose ps
```

The MySQL service should be healthy. On a new volume, Compose's MySQL configuration creates the database and application user.

The script uses the resolved Compose settings so Java receives matching database credentials. **Spring Boot does not automatically load this project's .env file.** Repeat this block before starting Java in a new PowerShell window.

Existing shell variables take precedence over .env in Compose. If effective settings differ from the file, check for old DB_* or MYSQL_ROOT_PASSWORD variables in that terminal.

### C. Start Spring Boot

In that same window:

```powershell
mvn.cmd spring-boot:run
```

Keep the terminal open and wait for the application-started message.

- API base URL: http://localhost:8080
- Inventory endpoint: http://localhost:8080/api/ducks
- Flyway creates the tables on first startup.
- A fresh database has no sample inventory.

Use a fresh schema for initial setup. If migration validation fails against an existing schema, inspect the migration error before changing its history.

## 4. Start the React UI

Open a **second PowerShell window**:

```powershell
Set-Location 'C:$path\duck-warehouse-react-ui\duck-warehouse-ui'
npm.cmd ci
if ($LASTEXITCODE -ne 0) {
    throw 'Frontend dependency installation failed.'
}
npm.cmd run dev
```

Open **http://localhost:5173** and keep this terminal open.

Vite forwards browser requests under /api to http://localhost:8080. This local setup does not need separate backend CORS configuration.

### Change the backend address or port

For example, stop the API with Ctrl+C, then start it on port 8081 in its configured terminal:

```powershell
$env:SERVER_PORT = '8081'
mvn.cmd spring-boot:run
```

In the frontend folder, create or edit its own .env file:

```powershell
if (-not (Test-Path -LiteralPath '.env')) {
    Copy-Item -LiteralPath '.env.example' -Destination '.env'
}
notepad .env
```

Set:

```dotenv
API_PROXY_TARGET=http://localhost:8081
```

Restart npm.cmd run dev after changing that file. The frontend and backend .env files are separate; database credentials belong in the backend configuration.

## 5. Check that everything works

Open a **third PowerShell window** while MySQL and the API are running.

```powershell
Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:8080/api/ducks' |
    Select-Object StatusCode, Content
```

Expect HTTP 200 and a JSON array, initially [] for an empty database. The backend root URL / is not the inventory endpoint.

Add sample stock:

```powershell
$duckBody = @{
    color = 'Red'
    size = 'Large'
    price = '10.00'
    quantity = 5
} | ConvertTo-Json

$addRequest = @{
    Uri = 'http://localhost:8080/api/ducks'
    Method = 'Post'
    ContentType = 'application/json'
    Body = $duckBody
}
$createdDuck = Invoke-RestMethod @addRequest
$createdDuck | Format-List
```

This writes stock. Repeating the request adds another 5 units to the same active color/size/price variant. Refresh the UI to see the record.

Request a quote:

```powershell
$quoteBody = @{
    color = 'Red'
    size = 'Large'
    quantity = 2
    destinationCountry = 'USA'
    shippingMode = 'Land'
} | ConvertTo-Json

$quoteRequest = @{
    Uri = 'http://localhost:8080/api/store/orders'
    Method = 'Post'
    ContentType = 'application/json'
    Body = $quoteBody
}
Invoke-RestMethod @quoteRequest | ConvertTo-Json -Depth 10
```

Quotes do not change stock. Adjust these example URLs if you changed SERVER_PORT.

Case-sensitive labels:

- Colors: Red, Green, Yellow, Black.
- Sizes: XLarge, Large, Medium, Small, XSmall.
- Shipping modes: Land, Sea, Air.

## 6. Alternative: use local MySQL

Use this section **instead of the Docker database steps**.

1. Start MySQL 8.4 locally.
2. Open backend database/create_database.sql in MySQL Workbench and execute it as an administrator. It creates duck_warehouse, but not users or tables.
3. For a new local development user, run the following SQL with your own password.

```sql
CREATE USER 'duck_app'@'localhost'
    IDENTIFIED BY 'replace-with-your-local-password';
GRANT ALL PRIVILEGES ON duck_warehouse.*
    TO 'duck_app'@'localhost';
```

For an existing user, reuse its actual credentials and grant the required schema access. The schema-scoped grant above supports local Flyway migrations and application operations.

Open PowerShell:

```powershell
Set-Location 'C:$path\duck-warehouse-springboot\duck-warehouse-api'
$env:DB_HOST = 'localhost'
$env:DB_PORT = '3306'
$env:DB_DATABASE = 'duck_warehouse'
$env:DB_USERNAME = 'duck_app'
$env:DB_PASSWORD = 'replace-with-your-local-password'
mvn.cmd spring-boot:run
```

Use the password configured in MySQL. Flyway creates the tables. Start the UI using section 4.

## 7. Automated tests

### Backend tests without a database

```powershell
Set-Location 'C:$path\duck-warehouse-springboot\duck-warehouse-api'
mvn.cmd test
```

### Frontend tests

```powershell
Set-Location 'C:$path\duck-warehouse-react-ui\duck-warehouse-ui'
npm.cmd test
```

### Optional database integration tests

As a database administrator, create a separate duck_warehouse_test database and grant your test application user access. Compose initializes only the main application database, not this test schema.

**These tests delete duck rows in the selected test database before each test.** Always use a dedicated database ending in _test. Flyway runs before the tests' cleanup-name safety check, so never set TEST_DB_DATABASE to an application database.

From the backend folder, set DB_HOST, DB_PORT, DB_USERNAME and DB_PASSWORD for the test database server/user, then run:

```powershell
$env:RUN_DB_TESTS = 'true'
$env:TEST_DB_DATABASE = 'duck_warehouse_test'
try {
    mvn.cmd verify
    if ($LASTEXITCODE -ne 0) {
        throw 'Backend verification failed.'
    }
}
finally {
    Remove-Item Env:RUN_DB_TESTS -ErrorAction SilentlyContinue
    Remove-Item Env:TEST_DB_DATABASE -ErrorAction SilentlyContinue
}
```

With RUN_DB_TESTS unset, MySQL integration tests are skipped. These are correctness tests, not 10,000-request load tests.

## 8. Build and run packaged output

### Backend JAR

Stop the development API, then use its terminal with database environment variables loaded:

```powershell
Set-Location 'C:$path\duck-warehouse-springboot\duck-warehouse-api'
mvn.cmd clean package
if ($LASTEXITCODE -ne 0) {
    throw 'Backend package build failed.'
}
java -jar target/duck-warehouse-api-1.0.0.jar
```

MySQL must still be running.

### Frontend build and local preview

```powershell
Set-Location 'C:$path\duck-warehouse-react-ui\duck-warehouse-ui'
npm.cmd run build
if ($LASTEXITCODE -ne 0) {
    throw 'Frontend build failed.'
}
npm.cmd run preview
```

The build creates dist/. Local preview is at http://localhost:4173 and this project's preview configuration also proxies /api to the backend.

For deployment, serve dist/ through a web server that forwards /api to Spring Boot, or place its contents in the backend's src/main/resources/static/ and rebuild the JAR. The Vite proxy is not embedded in dist/. Preview is for checking builds locally, rather than production hosting. See the [Vite deployment guide](https://vite.dev/guide/static-deploy.html).

## 9. Stop and restart

- API: Ctrl+C in its terminal.
- UI or preview: Ctrl+C in its terminal.
- Docker MySQL: run this from the backend folder.

```powershell
Set-Location 'C:$path\duck-warehouse-springboot\duck-warehouse-api'
docker compose --env-file .env down
```

The MySQL named volume retains data. Restart by repeating section 3B, section 3C, then section 4. npm.cmd ci is needed initially and after dependency/lockfile changes; it need not run on every restart.

Changing passwords or database names in .env does not update an already initialized MySQL volume's accounts or schemas. Keep the existing database configuration and credentials consistent.

## 10. Troubleshooting

| Problem | Check or fix |
| --- | --- |
| Java or Maven not found | Install the full JDK and Maven; correct JAVA_HOME/PATH and reopen PowerShell. Check mvn.cmd -version. |
| Unsupported Node version | Use Node.js 22.12.0 or newer, then npm.cmd ci. |
| Docker engine unavailable | Start Docker Desktop with Linux containers. |
| Compose file not found | Run Docker commands in duck-warehouse-springboot/duck-warehouse-api. |
| MySQL is not healthy | Inspect docker compose logs --tail 100 mysql from the backend folder. |
| Port 3306 is busy | Use local MySQL, or set backend .env DB_PORT to an unused host port such as 3307; repeat section 3B and restart Java. |
| Access denied or missing DB_PASSWORD | Reload settings in Java's PowerShell terminal; check the initialized database user's actual password. |
| Flyway startup failure | Inspect the exact error, schema contents, and user privileges. Use a fresh development schema for initial setup. |
| UI cannot load inventory | Check /api/ducks on the API directly; confirm MySQL, Spring Boot, API_PROXY_TARGET, and a restarted Vite server. |
| Port 8080 is busy | Set SERVER_PORT and update API_PROXY_TARGET as in section 4. |
| Port 5173 or 4173 is busy | Stop the other UI instance; Vite's strictPort configuration reports a conflict. |
| Dependency downloads fail | Check registry access and organizational proxy settings. Preserve package-lock.json and use npm.cmd ci. |

## Further documentation

- [Backend API contract and business rules](duck-warehouse-springboot/duck-warehouse-api/README.md)
- [Frontend behavior and source structure](duck-warehouse-react-ui/duck-warehouse-ui/README.md)
- [10,000 concurrent requests guide](Duck_Warehouse_10000_Concurrent_Requests_Guide.docx)
- [Maven installation](https://maven.apache.org/install.html)

The capacity guide describes proposed changes and load-test criteria. Creating these instructions has not applied those changes or verified 10,000-request capacity.

