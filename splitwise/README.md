# Splitwise

Track shared trip expenses, split them smartly, scan bills with OCR, and see exactly who owes whom.

## Stack
- **Frontend:** React (Vite)
- **Backend:** Spring Boot 3, Java 17, Spring Data JPA, H2 (swap for Postgres/MySQL any time)
- **OCR:** Tesseract through Tess4J

## Features
- Create trips and add friends
- Add expenses and split them **equally, by percentage, or by exact amount** (cents are never lost)
- **Scan a bill photo**: the total and merchant are filled in for you
- **Trip summary**: total spent, spend by category, each person's balance
- **Settle up**: the fewest possible payments to clear everyone's debts, with a "Mark paid" button

## How the money maths works
Each expense records who paid and how much each person owes (`expense_shares`).
Balance = what you paid - what you owe + settlements you made - settlements you received.
`SplitCalculator.simplify()` then matches the biggest debtor with the biggest creditor until everyone is square.
All of that logic lives in `SplitCalculator.java` and is unit tested.

## Run it

**Backend** (needs Java 17+ and Maven)
```
cd backend
mvn spring-boot:run
```
API runs on http://localhost:8080. Run the tests with `mvn test`.

**Frontend**
```
cd frontend
npm install
npm run dev
```
Open http://localhost:5173.

## OCR setup (for bill scanning)
1. Install Tesseract: on Windows grab the installer from the UB Mannheim build; on Mac `brew install tesseract`; on Ubuntu `sudo apt install tesseract-ocr`.
2. Download `eng.traineddata` from the tessdata repo on GitHub (`tesseract-ocr/tessdata`) and put it in `backend/tessdata/`.
3. Restart the backend. Everything else works without OCR, you'll just see a friendly error on scan.

## API
| Method | Path | What it does |
|---|---|---|
| GET/POST | `/api/trips` | List / create trips |
| GET | `/api/trips/{id}` | Full summary (balances, categories, settle-up) |
| POST | `/api/trips/{id}/members` | Add a person |
| POST | `/api/trips/{id}/expenses` | Add an expense |
| DELETE | `/api/trips/{id}/expenses/{expenseId}` | Remove an expense |
| POST | `/api/trips/{id}/settlements` | Record a payment |
| POST | `/api/ocr/scan` | Upload a bill image, get merchant + total |

## Ideas to extend
- User accounts and invites
- Multiple currencies
- Line-item OCR so each dish can be assigned to a person
