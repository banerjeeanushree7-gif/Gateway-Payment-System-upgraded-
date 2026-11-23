# Gateway (demo)

Java servlets + MongoDB. Cards, UPI and net banking are **simulated**: nothing is charged.

## Run
1. Start MongoDB (default `mongodb://localhost:27017`, or set `MONGO_URI`).
2. `mvn package` runs the unit tests and builds `target/PaymentGateway.war`.
3. Drop the WAR into Tomcat 9 (`webapps/`). For Tomcat 10+, switch the `javax.servlet` imports to `jakarta.servlet`.
4. Open `http://localhost:8080/PaymentGateway/` and sign in.

Demo login defaults to `test@user.com` / `12345`. Override with `PG_DEMO_EMAIL` and `PG_DEMO_PASSWORD`.

## Test inputs
| Case | Input |
|---|---|
| Success | card `4111 1111 1111 1111`, any future expiry, CVV `123` |
| Declined | any valid card number ending in `5` (simulated rule) |
| Amex | `3714 496353 98431` with a 4-digit CVV |
| UPI | `name@okaxis` |
| Net banking | any listed bank, 10-16 digit account, IFSC like `SBIN0123456` |

## What changed in v2
- Amounts parsed as `BigDecimal` and stored as `Decimal128` (`NaN`, `Infinity`, `1e9` rejected)
- All validation is server-side and unit tested (`Validators`, `ValidatorsTest`)
- `AuthFilter` protects every page, sets security headers (CSP, no-store) and checks a CSRF token on all POSTs
- Login: PBKDF2 hash, session id rotated on login, 5 attempts per 5 minutes per IP
- Logout is POST-only; history is a JSON API rendered with `textContent` (no stored XSS)
- One shared `MongoClient`; transaction ids; one-time form nonces stop double submits
- Receipt page shows the real result and failure reason

## Still to do before real use
Serve over HTTPS and set the `secure` cookie flag, use a real users collection, and never handle raw card numbers yourself: use a PSP's hosted fields (Razorpay, Stripe) to stay out of PCI-DSS scope.

## Front end (v2.1)
Light-purple "Gateway" look with a home screen of payment tiles. Successful payments can be downloaded as a PDF statement from the result page and from History. The PDF is generated in the browser (`app.js`), so no extra Java library is needed.
