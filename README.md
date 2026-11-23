# 💳 Gateway Payment System

![GitHub stars](https://img.shields.io/github/stars/banerjeeanushree7-gif/Gateway-Payment-System-upgraded-?style=for-the-badge)
![GitHub forks](https://img.shields.io/github/forks/banerjeeanushree7-gif/Gateway-Payment-System-upgraded-?style=for-the-badge)
![GitHub issues](https://img.shields.io/github/issues/banerjeeanushree7-gif/Gateway-Payment-System-upgraded-?style=for-the-badge)
![GitHub last commit](https://img.shields.io/github/last-commit/banerjeeanushree7-gif/Gateway-Payment-System-upgraded-?style=for-the-badge)
![GitHub repo size](https://img.shields.io/github/repo-size/banerjeeanushree7-gif/Gateway-Payment-System-upgraded-?style=for-the-badge)
![GitHub language count](https://img.shields.io/github/languages/count/banerjeeanushree7-gif/Gateway-Payment-System-upgraded-?style=for-the-badge)
![GitHub top language](https://img.shields.io/github/languages/top/banerjeeanushree7-gif/Gateway-Payment-System-upgraded-?style=for-the-badge)

A mobile-first payment app in the style of modern UPI apps. Sign in, pick a way to pay (UPI, mobile number, bank account or card), see exactly which server-side checks ran, and download a **PDF statement** for every successful payment.

The project comes in two versions that share the same rules and look:

| Version | What it is | Where it runs |
|---|---|---|
| **Browser demo** | One static `index.html`, no backend | Any static host (Netlify, GitHub Pages) |
| **Java edition** | Servlets + MongoDB with real server-side validation | Tomcat 9 + MongoDB |

> ⚠️ **All payments are simulated.** No money moves and no real card, UPI or bank network is contacted.

---

## 🚀 Live Demo

🌐 **Website:** https://YOUR-SITE.netlify.app

Sign in with the demo account below, then try the **Try a test case** buttons inside each payment sheet.

---

## 📌 Features

### 🔐 Authentication
- Sign-in screen that must be passed before the app opens
- Invalid-credential handling with inline error messages
- Java edition: PBKDF2 password hash, session ID rotation on login, and a limit of 5 attempts per 5 minutes per IP

### 🏠 Home (UPI-app style)
- Light-purple theme with automatic dark mode
- Swipeable, auto-rotating banner with tappable dots
- Tile grid: Scan & Pay, To Mobile, To UPI ID, To Bank A/c, Pay by Card, History, Statements, Receive Money
- Recent payments list and a floating **Scan any QR** button
- Profile sheet with sign out

### 💸 Payments
- **Card:** Luhn check, MM/YY expiry that must not be in the past, 3-digit CVV (4 digits for American Express), auto-formatted card number
- **UPI ID:** format check such as `name@okaxis`
- **Mobile number:** 10 digits starting with 6 to 9
- **Net banking:** bank whitelist, 10 to 16 digit account number, IFSC format check
- Amounts must be plain decimals (up to 2 places, maximum 1,000,000); `NaN`, `Infinity` and `1e9` are rejected
- Card numbers, UPI IDs and account numbers are masked before they are stored
- Result screen with a collapsible list of every server-side check

### 🧾 Statements
- **Download statement (PDF)** on every successful payment
- PDF button on each successful row in History and Statements
- PDFs are generated in the browser with a small built-in writer, so no PDF library is needed

### 📜 History
- Newest payments first, with status and amount
- Clear history option in the demo

### 🌙 Additional Features
- Responsive, mobile-first layout
- Dark mode support
- Local Storage integration (browser demo)
- Toast notifications and slide-up sheets
- Keyboard support (Esc closes sheets) and reduced-motion support

---

## 🔒 Security (Java edition)

- `AuthFilter` guards every page and API, so pages such as the result screen cannot be opened without signing in
- CSRF token required on every POST
- Security headers on every response: Content-Security-Policy, `X-Frame-Options`, `no-store`, `nosniff`
- Output is rendered with `textContent`, which prevents stored XSS in the history list
- One-time form nonces stop double submits from creating duplicate payments
- Logout is POST-only
- MongoDB address comes from the `MONGO_URI` environment variable

---

## 🛠️ Tech Stack

### Frontend
- HTML5
- CSS3
- JavaScript (ES6)

### Backend (Java edition)
- Java 11
- Servlets (`javax.servlet` 4.0, Tomcat 9)
- MongoDB Java driver 4.11
- Maven
- JUnit 5

### Storage
- Local Storage API (browser demo)
- MongoDB (Java edition)

---

## 📂 Project Structure

```
Gateway-Payment-System-upgraded-/
│
├── netlify/
│   └── index.html                       # complete browser demo (single file)
│
├── PaymentGateway/                      # Java edition (Maven project)
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/payment/
│   │   │   │   ├── AppContextListener.java   # creates the shared MongoClient
│   │   │   │   ├── AuthFilter.java           # login gate, CSRF, security headers
│   │   │   │   ├── CsrfServlet.java
│   │   │   │   ├── LoginServlet.java
│   │   │   │   ├── LogoutServlet.java
│   │   │   │   ├── PaymentServlet.java
│   │   │   │   ├── HistoryServlet.java       # JSON API for history and receipts
│   │   │   │   ├── PaymentRepository.java    # all MongoDB access
│   │   │   │   └── Validators.java           # pure validation and masking rules
│   │   │   │
│   │   │   └── webapp/
│   │   │       ├── WEB-INF/web.xml
│   │   │       ├── login.html
│   │   │       ├── paymentOptions.html
│   │   │       ├── payment.html
│   │   │       ├── upi.html
│   │   │       ├── netbanking.html
│   │   │       ├── result.html
│   │   │       ├── history.html
│   │   │       ├── error.html
│   │   │       ├── style.css
│   │   │       └── app.js
│   │   │
│   │   └── test/java/com/payment/
│   │       └── ValidatorsTest.java
│   │
│   ├── pom.xml
│   └── .gitignore
│
├── LICENSE
└── README.md
```

---

## 🚀 Getting Started

### Clone Repository

```bash
git clone https://github.com/banerjeeanushree7-gif/Gateway-Payment-System-upgraded-.git
cd PaymentGateway
```

### Option 1: Browser demo (no installation)

Open `netlify/index.html` in your browser. Nothing else is needed.

### Option 2: Java edition

**Requirements:** JDK 11+, Maven, MongoDB, Tomcat 9

1. Start MongoDB (default `mongodb://localhost:27017`, or set `MONGO_URI`).
2. Build and run the unit tests (from the `PaymentGateway` folder):
   ```bash
   cd PaymentGateway
   mvn package
   ```
3. Copy `target/PaymentGateway.war` into Tomcat's `webapps/` folder.
4. Open `http://localhost:8080/PaymentGateway/` and sign in.

> For Tomcat 10 or newer, switch the `javax.servlet` imports to `jakarta.servlet` and update the servlet API dependency in `pom.xml`.

### Environment variables (Java edition)

| Variable | Purpose | Default |
|---|---|---|
| `MONGO_URI` | MongoDB connection string | `mongodb://localhost:27017` |
| `PG_DEMO_EMAIL` | Demo login email | `test@user.com` |
| `PG_DEMO_PASSWORD` | Demo login password | `12345` |

---

## 🔑 Demo Login

**Email**

```
test@user.com
```

**Password**

```
12345
```

---

## 🧪 Test Inputs

| Case | What to enter | Expected result |
|---|---|---|
| Successful card | `4111 1111 1111 1111`, expiry `12/30`, CVV `123` | ✅ Success |
| Declined card | `4111 1111 1111 1145` (any valid number ending in 5) | ❌ Declined by test rule |
| Expired card | `4111 1111 1111 1111`, expiry `01/20` | ❌ Expired |
| American Express | `3714 496353 98431`, 4-digit CVV | ✅ Success |
| Bad amount | `NaN`, `1e9`, `-5` or `1.234` | ❌ Amount rejected |
| UPI | `asha@okaxis` | ✅ Success |
| Bad UPI | `@okaxis` | ❌ Invalid UPI ID |
| Mobile | `9958736314` | ✅ Success |
| Net banking | Bank `SBI`, account `1234567890`, IFSC `SBIN0123456` | ✅ Success |

---

## ☁️ Deploy to Netlify

1. Go to [app.netlify.com/drop](https://app.netlify.com/drop).
2. Drop in `index.html` (or a `.zip` that contains it).
3. Open the generated `*.netlify.app` link.
4. To rename it, open **Site configuration → Change site name**.

---

## 📈 Future Improvements

- Real users collection with registration and password reset
- Hosted card fields from a payment provider (Razorpay or Stripe) so raw card data never reaches the server
- HTTPS-only deployment with the `secure` cookie flag
- Email and SMS payment receipts
- Refunds and payment status webhooks
- Role-based access control and an admin dashboard
- Filters, search and pagination in History
- Real QR scanning with the device camera
- Docker setup for one-command local runs

---

## 🎯 Learning Outcomes

This project demonstrates:

- Mobile-first, responsive UI design
- Server-side input validation and unit testing
- Servlet filters, sessions and CSRF protection
- Working with MongoDB from Java
- Client-side PDF generation
- Dark mode with CSS variables
- Secure handling and masking of sensitive data
- Separating pure logic from servlet and database code

---

## 🤝 Contributing

Contributions are welcome.

1. Fork the repository
2. Create your feature branch
   ```bash
   git checkout -b feature-name
   ```
3. Commit your changes
   ```bash
   git commit -m "Add new feature"
   ```
4. Push to GitHub
   ```bash
   git push origin feature-name
   ```
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

## 👩‍💻 Author

**Anushree Banerjee**

GitHub: https://github.com/banerjeeanushree7-gif

LinkedIn: https://www.linkedin.com/in/YOUR-PROFILE

---

⭐ If you found this project helpful, don't forget to star the repository.

