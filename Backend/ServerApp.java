package Backend;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

public class ServerApp {
    public void start() { }

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    // When processing a payment webhook that needs to attach an intent:
    public void attachPaymentIntent(LoadRequest loadRequest, String paymentIntentID, Instant expiresAt) {
        boolean attached = loadRequest.attachIntent(paymentIntentID, expiresAt);
        
        if (attached) {
            // Schedule the expiration task
            ScheduledFuture<?> expirationTask = loadRequest.scheduleExpiration();
            
            // Store it if you need to cancel early (e.g., when payment completes)
            // Map<String, ScheduledFuture<?>> expirationTasks = new ConcurrentHashMap<>();
            // expirationTasks.put(loadRequest.getLoadID(), expirationTask);
        }
    }

    // When a payment webhook arrives confirming payment:
    public void handlePaymentWebhook(LoadRequest loadRequest, ScheduledFuture<?> expirationTask) {
        if (expirationTask != null && !expirationTask.isDone()) {
            expirationTask.cancel(false); // Cancel the expiration task
        }
        
        boolean paid = loadRequest.markPaid();
        if (paid) {
            // Credit the card, log the transaction, etc.
            creditCard(
                loadRequest.getOwnerAccountID(),
                loadRequest.getCardUID(),
                loadRequest.getAmountInCentavos(),
                loadRequest.getPaymentIntentID()
            );
        }
    }

    // Graceful shutdown
    public void shutdown() {
        scheduler.shutdown();
        // Optionally: scheduler.awaitTermination(10, TimeUnit.SECONDS);
    }

    private void creditCard(String ownerAccountID, String cardUID, long amountInCentavos, String paymentIntentID) {
        // Implementation to credit the transit card
    }
}