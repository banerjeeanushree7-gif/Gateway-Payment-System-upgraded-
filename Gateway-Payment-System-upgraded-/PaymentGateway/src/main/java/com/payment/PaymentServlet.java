package com.payment;

import javax.servlet.http.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PaymentServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    @SuppressWarnings("unchecked")
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);          // AuthFilter guarantees a logged-in session
        String user = (String) session.getAttribute("user");
        PaymentRepository repo = (PaymentRepository) getServletContext().getAttribute(AppContextListener.REPO);

        // Double-submit protection: each form carries a one-time nonce.
        String nonce = req.getParameter("_nonce");
        Map<String, String> done;
        synchronized (this) {
            done = (Map<String, String>) session.getAttribute("nonces");
            if (done == null) {
                done = new ConcurrentHashMap<>();
                session.setAttribute("nonces", done);
            }
        }
        if (nonce != null && nonce.matches("[a-f0-9]{8,64}")) {
            if (done.size() > 50) done.clear();
            String prev = done.putIfAbsent(nonce, "pending");
            if (prev != null) {                                // repeat of an earlier submit
                resp.sendRedirect("pending".equals(prev) ? "history.html" : "result.html?id=" + prev);
                return;
            }
        } else {
            nonce = null;
        }

        String method = trim(req.getParameter("method")).toLowerCase();
        String savedMethod = method.equals("card") || method.equals("upi") || method.equals("netbank")
                ? method : "unknown";                          // never store raw user input as the method
        BigDecimal amount = Validators.parseAmount(req.getParameter("amount"));
        String masked = "-";
        String error = null;

        if (amount == null) {
            error = "Amount must be a plain number from 0.01 to 1,000,000 (max 2 decimals).";
        } else {
            switch (method) {
                case "card": {
                    String pan = Validators.digitsOnly(req.getParameter("cardNumber"));
                    String name = trim(req.getParameter("cardName"));
                    if (name.isEmpty() || name.length() > 60) error = "Enter the name on the card.";
                    else if (!Validators.panFormat(pan) || !Validators.luhn(pan)) error = "Card number is not valid.";
                    else if (!Validators.expiryValid(trim(req.getParameter("expiry")), YearMonth.now(ZoneOffset.UTC)))
                        error = "Card has expired, or the date is not in MM/YY format.";
                    else if (!Validators.cvvValid(trim(req.getParameter("cvv")), pan)) error = "CVV is not valid.";
                    else {
                        masked = Validators.maskCard(pan);
                        if (pan.endsWith("5")) error = "Card declined (test rule: numbers ending in 5 are declined).";
                    }
                    break;
                }
                case "upi": {
                    String upi = trim(req.getParameter("upiId"));
                    if (!Validators.upiValid(upi)) error = "UPI ID is not valid (example: name@okaxis).";
                    else masked = Validators.maskUpi(upi);
                    break;
                }
                case "netbank": {
                    String acc = trim(req.getParameter("accNumber"));
                    if (!Validators.bankValid(req.getParameter("bank"))) error = "Choose a bank from the list.";
                    else if (!Validators.accountValid(acc)) error = "Account number must be 10 to 16 digits.";
                    else if (!Validators.ifscValid(req.getParameter("ifsc"))) error = "IFSC code is not valid (example: SBIN0123456).";
                    else masked = Validators.maskAccount(acc);
                    break;
                }
                default:
                    error = "Unknown payment method.";
            }
        }

        try {
            String txn = repo.save(user, savedMethod, masked, amount,
                    error == null ? "SUCCESS" : "FAILED", error == null ? "OK" : error);
            if (nonce != null) done.put(nonce, txn);
            resp.sendRedirect("result.html?id=" + txn);
        } catch (RuntimeException e) {
            if (nonce != null) done.remove(nonce);             // allow a retry
            log("Could not save payment", e);                  // never log card data
            resp.sendRedirect("result.html?error=db");
        }
    }

    private static String trim(String s) { return s == null ? "" : s.trim(); }
}
