package com.payment;

import org.bson.Document;
import org.bson.types.Decimal128;

import javax.servlet.http.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** JSON API: GET /api/history (latest 50, newest first) or GET /api/history?id=<txnId>. */
public class HistoryServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String user = (String) req.getSession(false).getAttribute("user");
        PaymentRepository repo = (PaymentRepository) getServletContext().getAttribute(AppContextListener.REPO);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String id = req.getParameter("id");
        if (id != null) {
            Document d = id.matches("[0-9a-f-]{36}") ? repo.find(user, id) : null;
            if (d == null) { resp.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
            resp.getWriter().write(view(d).toJson());
            return;
        }
        List<Document> out = new ArrayList<>();
        for (Document d : repo.history(user, 50)) out.add(view(d));
        resp.getWriter().write(new Document("payments", out).toJson());
    }

    /** Plain strings only (amount as text, time as ISO-8601 UTC) so the browser needs no BSON knowledge. */
    private static Document view(Document d) {
        Decimal128 a = d.get("amount", Decimal128.class);
        Date t = d.getDate("createdAt");
        return new Document("txnId", d.getString("txnId"))
                .append("method", d.getString("method"))
                .append("id", d.getString("id"))
                .append("amount", a == null ? null : a.bigDecimalValue().toPlainString())
                .append("status", d.getString("status"))
                .append("reason", d.getString("reason"))
                .append("time", t == null ? null : t.toInstant().toString());
    }
}
