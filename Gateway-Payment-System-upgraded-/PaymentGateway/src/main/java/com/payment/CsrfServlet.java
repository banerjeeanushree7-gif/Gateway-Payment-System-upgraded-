package com.payment;

import javax.servlet.http.*;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

/** Hands the page its per-session CSRF token as JSON. app.js adds it to every form. */
public class CsrfServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final SecureRandom RNG = new SecureRandom();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession s = req.getSession();
        String token = (String) s.getAttribute("csrf");
        if (token == null) {
            byte[] b = new byte[24];
            RNG.nextBytes(b);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(b);
            s.setAttribute("csrf", token);
        }
        resp.setContentType("application/json");
        resp.getWriter().write("{\"token\":\"" + token + "\"}");
    }
}
