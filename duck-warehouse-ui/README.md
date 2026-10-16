# Duck Warehouse — React UI

React.js + Vite frontend for the supplied Spring Boot backend. Includes the
warehouse listing, one shared add/edit form, logical-delete confirmation,
responsive layout, client validation, and server error handling. No store UI
is included because the store requirement is backend only.

## Setup on Windows

Install Node.js 22.12 or newer. Start the Spring Boot backend on port 8080
using its README; ensure MySQL is configured and running.

Extract this project into:

`C:\Users\JANARDHANA.NK\Downloads\log\duck-warehouse-ui`

Open a terminal in that folder:

```powershell
npm ci
npm run dev
```

Open http://localhost:5173. If the backend uses a different address:

```powershell
Copy-Item .env.example .env
notepad .env
```

Set `API_PROXY_TARGET` to the backend address and restart Vite. Requests go
through Vite's `/api` proxy, so the Spring Boot backend needs no CORS changes.
This is a real API-connected UI; it has no hardcoded inventory or localStorage
substitute. When the backend is unavailable, it displays a connection error.

## Backend endpoints used

| Operation | Endpoint |
|---|---|
| List active ducks | `GET /api/ducks` |
| Create or merge quantity | `POST /api/ducks` |
| Edit price and quantity | `PUT /api/ducks/{id}` |
| Logical delete | `DELETE /api/ducks/{id}` |

Data follows the supplied backend's lower camelCase contract. Labels are
case-sensitive: Red/Green/Yellow/Black; XLarge/Large/Medium/Small/XSmall.

The UI keeps the backend's quantity ordering. Add sends `color`, `size`,
`price`, and `quantity`; edit sends only `price` and `quantity`. Read-only
color and size remain visible while editing. Decimal prices are sent as
strings to preserve cents. Add quantities must be positive integers; editing
allows zero. Backend validation remains authoritative.

After each successful mutation, the UI refreshes from the backend so merged
quantities and database IDs are reflected correctly. Writes are never
automatically retried. Interrupted writes prompt the user to inspect inventory
before submitting again. If refreshing fails, the UI marks any displayed data
as potentially stale and disables mutation controls until a successful refresh.

Dialogs use native `dialog` focus containment, accessible labels, keyboard
Escape dismissal when idle, and focus restoration to the opening button.
The delete dialog initially focuses Cancel. Save/delete controls are disabled
while requests are in progress to prevent accidental double submission.

## Verification and production build

```powershell
npm test
npm run build
npm run preview
```

Preview runs at http://localhost:4173 and uses the same backend proxy. The
build produces `dist/`. For actual deployment, serve `dist/` through a web
server that forwards `/api` to Spring Boot, or copy its contents into the
backend's `src/main/resources/static/` before packaging the backend. Vite's
development proxy is not included in the built static files.

The Node tests cover decimal precision, quantity limits, edit restrictions,
backend field errors, empty delete responses, and interrupted writes. They
do not replace live backend/database testing.

## Source structure

```text
src/App.jsx                    Warehouse screen and write orchestration
src/api/ducks.js                Fetch client and backend errors
src/hooks/useWarehouse.js       Inventory loading and request cancellation
src/lib/duckForm.js             Shared form validation and price formatting
src/components/DuckForm.jsx    Reused add/edit form
src/components/DuckTable.jsx   Inventory table and row actions
src/components/Modal.jsx       Accessible modal wrapper
src/components/DeleteConfirmation.jsx
src/styles.css                 Responsive styling
tests/                         Client business-boundary tests
```

No credentials are needed in this frontend. Do not put secrets in Vite
variables or commit `.env`, `node_modules`, or `dist`.
