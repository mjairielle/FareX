package Backend;

import java.time.LocalDateTime;

public class Transaction {
    private String uuid;
    private TransactionType type;
    private double amount, previousBalance, newBalance;
    private String method, cardLast4, status, referenceNo;
    private LocalDateTime timestamp;
}
