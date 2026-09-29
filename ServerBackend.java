public class ServerApp { 
    public void start() {} 
}

public class AuthHandler     { 
    public void register() {} 
    public void login() {} 
}
public class CardLoadHandler { 
    public void signAndReturnBalance() {} 
}
public class SyncHandler     { 
    public void receiveBatch() {} 
}
public class AdminHandler    { 
    public void applyAdminAction() {} 
}
public class TripHandler     { 
    public void queryRoutes() {} 
}
public class TransferHandler {
    public void receiveTransferRequest() {}
    public String signNewBalance() {}
    public void recordTransfer() {}
}

public class BalanceSigner { 
    public String sign(String cardUID, double balance) {} 
    public boolean verify() {} }
public class LedgerRepository { 
    public boolean existsUUID(String uuid) {} 
    public void append(Transaction t) {} }
public class CsvRepository<T> { 
    public List<T> readAll() {} 
    public void write(T item) {} }