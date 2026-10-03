package Backend;

import java.time.Instant;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class LoadRequest {
    private final String loadID;
    private final String ownerAccountID;
    private final String cardUID;
    private final long amountInCentavos;
    private final PaymentMethod method;
    private final Clock clock;
    private final ScheduledExecutorService scheduler;
    private final Instant createdAt;

    private volatile String paymentIntentID;
    private volatile LoadStatus status;
    private volatile Instant expiresAt;

    private static final long MAX_AMOUNT_CENTAVOS = 500_000_000;
    private static final long MIN_AMOUNT_CENTAVOS = 100;

    public LoadRequest(String loadID, String ownerAccountID, String cardUID, long amountInCentavos, PaymentMethod method, Clock clock, ScheduledExecutorService scheduler) {
        if (amountInCentavos < MIN_AMOUNT_CENTAVOS || amountInCentavos > MAX_AMOUNT_CENTAVOS) {
            throw new IllegalArgumentException("amountInCentavos must be between " + MIN_AMOUNT_CENTAVOS + " and " + MAX_AMOUNT_CENTAVOS + ", got " + amountInCentavos);
        }

        if (Objects.isNull(loadID) || loadID.isBlank()) {
            throw new IllegalArgumentException("loadID must be non-null and non-blank");
        }

        if (Objects.isNull(ownerAccountID) || ownerAccountID.isBlank()) {
            throw new IllegalArgumentException("ownerAccountID must be non-null and non-blank");
        }

        if (Objects.isNull(cardUID) || cardUID.isBlank()) {
            throw new IllegalArgumentException("cardUID must be non-null and non-blank");
        }

        if (Objects.isNull(method)) {
            throw new IllegalArgumentException("method must be non-null");
        }

        if (Objects.isNull(clock)) {
            throw new IllegalArgumentException("clock must be non-null");
        }

        this.loadID = loadID;
        this.ownerAccountID = ownerAccountID;
        this.cardUID = cardUID;
        this.amountInCentavos = amountInCentavos;
        this.method = method;
        this.clock = clock;
        this.scheduler = scheduler;
        this.status = LoadStatus.CREATED;
        this.createdAt = Instant.now(clock);
    }

    @Override
    public String toString() {
        return "LoadRequest{" +
                "loadID='" + loadID + '\'' +
                ", ownerAccountID='" + ownerAccountID + '\'' +
                ", cardUID='" + cardUID + '\'' +
                ", amountInCentavos=" + amountInCentavos +
                ", method=" + method +
                ", paymentIntentID='" + paymentIntentID + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", expiresAt=" + expiresAt +
                '}';
    }

    private boolean isOneOf(LoadStatus... statuses) {
        LoadStatus currentStatus = this.status;
        for (LoadStatus s : statuses) {
            if (currentStatus == s) {
                return true;
            }
        }
        return false;
    }

    public String getLoadID() {
        return loadID;
    }

    public String getOwnerAccountID() {
        return ownerAccountID;
    }

    public String getCardUID() {
        return cardUID;
    }

    public long getAmountInCentavos() {
        return amountInCentavos;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    //Volatile getters
    public LoadStatus getStatus() {
        return status;
    }

    public String getPaymentIntentID() {
        return paymentIntentID;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public synchronized boolean attachIntent(String paymentIntentID, Instant expiresAt) {
        if (Objects.isNull(paymentIntentID) || paymentIntentID.isBlank()) {
            throw new IllegalArgumentException("paymentIntentID must be non-null and non-blank");
        }
        if (Objects.isNull(expiresAt)) {
            throw new IllegalArgumentException("expiresAt must be non-null");
        }

        Instant now = Instant.now(clock);
        if (expiresAt.isBefore(now)) {
            throw new IllegalArgumentException("expiresAt must be after now, got " + expiresAt);
        }

        if (!isOneOf(LoadStatus.CREATED)) {
            return false;
        }
        
        this.paymentIntentID = paymentIntentID;
        this.expiresAt = expiresAt;
        this.status = LoadStatus.AWAITING_PAYMENT;
        return true;
    }

    public synchronized boolean markPaid() { 
        if(!isOneOf(LoadStatus.AWAITING_PAYMENT, LoadStatus.CANCELLED, LoadStatus.EXPIRED)) {
            return false;
        }
        this.status = LoadStatus.PAID;
        return true; 
    }
    public synchronized boolean markExpired() { 
        if(!isOneOf(LoadStatus.AWAITING_PAYMENT)) {
            return false;
        }
        this.status = LoadStatus.EXPIRED;
        return true; 
    }
    public synchronized boolean cancel() { 
        if(!isOneOf(LoadStatus.CREATED,LoadStatus.AWAITING_PAYMENT)) {
            return false;
        }
        this.status = LoadStatus.CANCELLED;
        return true; 
    }
    public synchronized boolean markFailed() {
        if(!isOneOf(LoadStatus.CREATED, LoadStatus.AWAITING_PAYMENT)) {
            return false;
        }
        this.status = LoadStatus.FAILED;
        return true;
    }

    public boolean isTerminal() {
        return isOneOf(LoadStatus.PAID, LoadStatus.FAILED);
    }
    
    public synchronized long secondsLeft() {
        if (status != LoadStatus.AWAITING_PAYMENT) {
            return 0;
        }

        Instant now = Instant.now(clock);
        long millisRemaining = Duration.between(now, expiresAt).toMillis();
        long secondsRemaining = (millisRemaining + 999) / 1000;

        return Math.max(0, secondsRemaining);
    }

    public ScheduledFuture<?> scheduleExpiration() {
        if (status != LoadStatus.AWAITING_PAYMENT) {
            throw new IllegalStateException("Cannot schedule expiration for load in " + status + " state");
        }

        long delaySeconds = secondsLeft();
        if (delaySeconds <= 0) {
            throw new IllegalStateException("expiresAt is not in the future");
        }

        return scheduler.schedule(this::markExpired, delaySeconds, TimeUnit.SECONDS);
    }
}