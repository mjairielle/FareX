package FareX;
public abstract class User {
    protected String accountID, name, password, phoneNumber, email;
    protected Role role;
    protected AccountStatus status;

    public boolean login(String emailOrContact, String password) {}
    public void logout() {}
}

public class Passenger extends User {
    private PassengerType passengerType;
    private Map<String, XPressCard> linkedCards;
    private Queue<PendingCredit> pendingCredits;
    private RewardsAccount rewards;

    public double inquireBalance(String cardUID) {}
    public void loadCard(String cardUID, PaymentMethod method, double amount) {}
    public void fetchLoad(String cardUID) {}
    public void reportLostCard(String cardUID) {}
    public void linkCard(String cardUID) {}
    public List<Transaction> viewTransactions() {}
    public List<Transaction> viewCardTransactions(String cardUID) {}
    public List<TripOption> planTrip(Stop a, Stop b) {}
    public void editProfile() {}
    public void changePin() {}
}

public class Operator extends User {
    private Vehicle assignedVehicle;
    private String operatorPin;
    private boolean dashboardUnlocked;
    private boolean onShift;
    private TapDevice tapDevice;
    private XPressCard payoutCard;

    public boolean unlockDashboard(String pin) {}
    public void lockDashboard() {}
    public void startShift() {}
    public void endShift() {}
    public void manualSync() {}

    public void linkPayoutCard(String cardUID) {}
    public double viewEarnings() {}
    public EarningsTransfer requestTransfer(double amount) {}
}

public class Admin extends User {
    public void suspendAccount(String accountID) {}
    public void reinstateAccount(String accountID) {}
    public void verifyDiscountType(String accountID, PassengerType type) {}
    public void reviewSubmittedStop(Stop jeepStop, boolean approve) {}
    public void editRoute(Route route) {}
    public void publishFareTable(FareTable faretable) {}
    public void assignVehicle(Vehicle vehicle, Operator operator) {}
    public void retireVehicle(Vehicle vehicle) {}
    public void createPromotion(Promotion promotion) {}
}