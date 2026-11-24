package com.payment;

import com.mongodb.client.MongoClients;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/** Creates the shared MongoClient once at startup and closes it on shutdown. */
public class AppContextListener implements ServletContextListener {
    public static final String REPO = "paymentRepository";

    @Override
    public void contextInitialized(ServletContextEvent e) {
        String uri = System.getenv().getOrDefault("MONGO_URI", "mongodb://localhost:27017");
        e.getServletContext().setAttribute(REPO, new PaymentRepository(MongoClients.create(uri)));
    }

    @Override
    public void contextDestroyed(ServletContextEvent e) {
        Object repo = e.getServletContext().getAttribute(REPO);
        if (repo instanceof PaymentRepository) ((PaymentRepository) repo).close();
    }
}
