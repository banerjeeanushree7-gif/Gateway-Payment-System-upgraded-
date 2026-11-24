package com.payment;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.types.Decimal128;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/** All MongoDB access lives here. One instance (and one MongoClient) per application. */
public class PaymentRepository implements AutoCloseable {
    private final MongoClient client;
    private final MongoCollection<Document> col;

    public PaymentRepository(MongoClient client) {
        this.client = client;
        this.col = client.getDatabase("paymentDB").getCollection("payments");
        col.createIndex(Indexes.compoundIndex(Indexes.ascending("user"), Indexes.descending("createdAt")));
        col.createIndex(Indexes.ascending("txnId"));
    }

    /** Saves one attempt and returns its transaction id. amount may be null for rejected input. */
    public String save(String user, String method, String maskedId, BigDecimal amount,
                       String status, String reason) {
        String txnId = UUID.randomUUID().toString();
        Document d = new Document("txnId", txnId)
                .append("user", user)
                .append("method", method)
                .append("id", maskedId == null ? "-" : maskedId)
                .append("amount", amount == null ? null : new Decimal128(amount))
                .append("status", status)
                .append("reason", reason)
                .append("createdAt", new Date());
        col.insertOne(d);
        return txnId;
    }

    public List<Document> history(String user, int limit) {
        return col.find(Filters.eq("user", user)).sort(Sorts.descending("createdAt"))
                .limit(limit).into(new ArrayList<>());
    }

    public Document find(String user, String txnId) {
        return col.find(Filters.and(Filters.eq("user", user), Filters.eq("txnId", txnId))).first();
    }

    @Override
    public void close() { client.close(); }
}
