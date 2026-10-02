package Backend;

import java.util.List;
import java.util.Map;
import java.util.Queue;

public class Passenger extends User {
    private PassengerType passengerType;
    private Map<String, XPressCard> linkedCards;
    private Queue<PendingCredit> pendingCredits;
    private RewardsAccount rewards;

    public double inquireBalance(String cardUID) { return 0.0; }
    public void loadCard(String cardUID, PaymentMethod method, double amount) { }
    public void fetchLoad(String cardUID) { }
    public void reportLostCard(String cardUID) { }
    public void linkCard(String cardUID) { }
    public List<Transaction> viewTransactions() { return null; }
    public List<Transaction> viewCardTransactions(String cardUID) { return null; }
    public List<TripOption> planTrip(Stop a, Stop b) { return null; }
    public void editProfile() { }
    public void changePin() { }
}
