package com.payment;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;

/**
 * One gate for the whole app:
 *  - security headers on every response
 *  - CSRF token check on every POST
 *  - login required for everything except the public paths below
 */
public class AuthFilter implements Filter {
    private static final Set<String> PUBLIC = Set.of(
            "/", "/login.html", "/error.html", "/style.css", "/app.js", "/api/csrf", "/LoginServlet");

    @Override
    public void doFilter(ServletRequest rq, ServletResponse rs, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) rq;
        HttpServletResponse resp = (HttpServletResponse) rs;

        resp.setHeader("Cache-Control", "no-store");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Referrer-Policy", "no-referrer");
        resp.setHeader("Content-Security-Policy", "default-src 'self'; frame-ancestors 'none'");

        String path = req.getServletPath();
        HttpSession session = req.getSession(false);

        if ("POST".equals(req.getMethod())) {
            String expected = session == null ? null : (String) session.getAttribute("csrf");
            String actual = req.getParameter("_csrf");
            if (expected == null || actual == null || !MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8))) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
                return;
            }
        }

        boolean loggedIn = session != null && session.getAttribute("user") != null;
        if (!loggedIn && !PUBLIC.contains(path)) {
            if (path.startsWith("/api/")) resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            else resp.sendRedirect(req.getContextPath() + "/login.html");
            return;
        }
        chain.doFilter(rq, rs);
    }
}
