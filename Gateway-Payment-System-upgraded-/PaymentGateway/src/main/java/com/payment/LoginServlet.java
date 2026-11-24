package com.payment;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Demo login. The credential is read from PG_DEMO_EMAIL / PG_DEMO_PASSWORD (defaults below are for
 * local demos only) and kept as a PBKDF2 hash. Replace with a users collection for real use.
 */
public class LoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final int ITERATIONS = 120_000;
    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 5 * 60 * 1000L;

    private static final String EMAIL = System.getenv().getOrDefault("PG_DEMO_EMAIL", "test@user.com");
    private static final byte[] SALT = new byte[16];
    private static final byte[] HASH;
    static {
        new SecureRandom().nextBytes(SALT);
        HASH = pbkdf2(System.getenv().getOrDefault("PG_DEMO_PASSWORD", "12345"));
    }

    /** ip -> {attempts, windowStart} */
    private final ConcurrentHashMap<String, long[]> attempts = new ConcurrentHashMap<>();

    private static byte[] pbkdf2(String password) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), SALT, ITERATIONS, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String ip = req.getRemoteAddr();
        long now = System.currentTimeMillis();
        long[] a = attempts.compute(ip, (k, v) -> (v == null || now - v[1] > WINDOW_MS)
                ? new long[]{1, now} : new long[]{v[0] + 1, v[1]});
        if (a[0] > MAX_ATTEMPTS) {
            resp.sendRedirect("login.html?error=locked");
            return;
        }

        String email = req.getParameter("email");
        String pass = req.getParameter("password");
        // Always hash, so timing doesn't reveal whether the email exists.
        boolean passOk = MessageDigest.isEqual(HASH, pbkdf2(pass == null ? "" : pass));
        boolean emailOk = email != null && MessageDigest.isEqual(
                EMAIL.getBytes(StandardCharsets.UTF_8), email.trim().getBytes(StandardCharsets.UTF_8));

        if (emailOk && passOk) {
            attempts.remove(ip);
            HttpSession session = req.getSession();
            req.changeSessionId();            // prevents session fixation
            session.setAttribute("user", EMAIL);
            resp.sendRedirect("paymentOptions.html");
        } else {
            resp.sendRedirect("login.html?error=1");
        }
    }
}
