package FareX;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class Passenger extends User {
    private PassengerType passengerType = PassengerType.REGULAR;
    private Map<String, XPressCard> linkedCards = new LinkedHashMap<>();
    private Queue<PendingCredit> pendingCredits;
    private RewardsAccount rewards;

    /** Shared, app-wide planner. Set once at startup; see setTripPlanner(). */
    private TripPlanner tripPlanner;

    public Passenger() {
        this.role = Role.PASSENGER;
    }

    public Passenger(String accountID, String name, PassengerType passengerType) {
        this();
        this.accountID = accountID;
        this.name = name;
        this.passengerType = (passengerType == null) ? PassengerType.REGULAR : passengerType;
    }

    public String getAccountID()            { return accountID; }
    public PassengerType getPassengerType() { return passengerType; }

    public Map<String, XPressCard> getLinkedCards() {
        return Collections.unmodifiableMap(linkedCards);
    }

    /**
     * Attach an already-loaded card to this account. Stand-in until
     * linkCard(cardUID) can look the card up from the card repository;
     * when it can, linkCard should end by calling this.
     */
    public void attachCard(XPressCard card) {
        if (card == null || card.getCardUID() == null) {
            throw new IllegalArgumentException("card has no UID");
        }
        if (card.getOwnerAccountID() != null && !card.getOwnerAccountID().equals(accountID)) {
            throw new IllegalArgumentException("card " + card.getCardUID()
                    + " belongs to another account");
        }
        linkedCards.put(card.getCardUID(), card);
    }

    /** The planner is shared by every passenger, so the app hands it in once. */
    public void setTripPlanner(TripPlanner tripPlanner) { this.tripPlanner = tripPlanner; }

    public double inquireBalance(String cardUID) { return requireCard(cardUID).getBalance(); }
    public void loadCard(String cardUID, PaymentMethod method, double amount) { }
    public void fetchLoad(String cardUID) { }
    public void reportLostCard(String cardUID) { }
    public void linkCard(String cardUID) { }
    public List<Transaction> viewTransactions() { return null; }
    public List<Transaction> viewCardTransactions(String cardUID) { return null; }
    /**
     * Up to three genuinely different journeys (cheapest, fastest, fewest
     * transfers), merged and deduped by TripPlanner. Never null; empty if
     * a and b are not connected. Show fares with fareFor(trip) so a
     * student, senior or PWD sees their discounted price.
     */
    public List<TripOption> planTrip(Stop a, Stop b) {
        if (tripPlanner == null) {
            throw new IllegalStateException("no TripPlanner set; call setTripPlanner() at startup");
        }
        return tripPlanner.planAllPreferences(a, b);
    }

    /** What this passenger actually pays for a trip, concession included. */
    public double fareFor(TripOption trip) {
        return trip.getTotalFareFor(passengerType);
    }

    /**
     * Pesos still needed on this card to take the trip, or 0.0 if the
     * balance covers it. Uses this passenger's fare, not the regular one.
     */
    public double shortfall(TripOption trip, String cardUID) {
        XPressCard card = requireCard(cardUID);
        if (!card.isUsable()) {
            throw new IllegalStateException("card " + cardUID + " is " + card.getStatus());
        }
        return TripPlanner.shortfall(trip, passengerType, card.getBalance());
    }

    public boolean canAfford(TripOption trip, String cardUID) {
        return shortfall(trip, cardUID) == 0.0;
    }

    private XPressCard requireCard(String cardUID) {
        XPressCard card = linkedCards.get(cardUID);
        if (card == null) {
            throw new IllegalArgumentException("card " + cardUID + " is not linked to this account");
        }
        return card;
    }

    public void editProfile() { }
    public void changePin() { }
}
