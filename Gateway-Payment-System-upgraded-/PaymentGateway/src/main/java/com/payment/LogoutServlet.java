package com.payment;

import javax.servlet.http.*;
import java.io.IOException;

/** POST only (CSRF-protected by AuthFilter), so a stray link can't log someone out. */
public class LogoutServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession s = req.getSession(false);
        if (s != null) s.invalidate();
        resp.sendRedirect("login.html");
    }
}
