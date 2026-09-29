package FareX;
public class Transaction {
    private String uuid;
    private TransactionType type;
    private double amount, previousBalance, newBalance;
    private String method, cardLast4, status, referenceNo;
    private LocalDateTime timestamp;
}

public class CsvStore {
    public void append(Transaction t) {}
    public List<Transaction> readAll() {}
}

public class SyncService {
    public void uploadBatch() {}
}