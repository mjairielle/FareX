package farex.shared.data;

import farex.shared.fare.*;
import farex.shared.txn.Txn;
import farex.shared.user.*;
import java.time.Instant;
import java.util.*;

/** Operator side tap logic. Who is on board is kept in boarding.csv so a restart or a second jeepney cannot lose or duplicate a rider. */
public class TapDesk {
    public record BoardedInfo(String cardUid, String passengerName, int stopIdx, String stopName, Instant time) {}

    private final Store store;
    private final String operatorId;
    private final List<RouteStop> stops;
    private final Map<String, BoardedInfo> boarded = new LinkedHashMap<>();

    public TapDesk(Store store, String operatorId) {
        this.store = store;
        this.operatorId = operatorId;
        this.stops = store.stops();
        for (String[] r : store.activeBoardings().values()) {       // pick up riders still on board after a restart
            int idx = Integer.parseInt(r[2]);
            if (r[1].equals(operatorId) && idx >= 0 && idx < stops.size())
                boarded.put(r[0], new BoardedInfo(r[0], nameOf(r[0]), idx, stops.get(idx).name(), java.time.Instant.parse(r[3])));
        }
    }

    private String nameOf(String cardUid) {
        return store.findUser(store.ownerOf(cardUid)).map(User::getName).orElse("Card " + Store.formatUid(cardUid));
    }

    public List<RouteStop> stops() { return stops; }
    public int onBoard() { return boarded.size(); }
    public List<BoardedInfo> boardedPassengers() { return new ArrayList<>(boarded.values()); }
    public void clearBoarded() {
        for (String uid : boarded.keySet()) store.logBoarding(uid, operatorId, 0, "ALIGHT");
        boarded.clear();
    }

    public String resolveCard(String identifier) {
        if (identifier == null || identifier.isBlank()) throw new IllegalArgumentException("Card or user ID is required");
        String clean = identifier.trim().replace(" ", "");
        if (store.ownerOf(clean) != null) return clean;
        if (clean.startsWith("USER:")) {
            clean = clean.substring(5);
        }
        List<String> userCards = store.cardsOf(clean);
        if (!userCards.isEmpty()) return userCards.get(0);
        return clean;
    }

    public BoardedInfo tapIn(String rawUid, int stopIdx) {
        String cardUid = resolveCard(rawUid);
        if (store.isCardFrozen(cardUid)) throw new IllegalStateException("Card is frozen / reported lost");
        if (boarded.containsKey(cardUid) || store.activeBoardings().containsKey(cardUid))
            throw new IllegalStateException("Card is already tapped in");
        if (stopIdx < 0 || stopIdx >= stops.size()) throw new IllegalArgumentException("Invalid stop index");
        if (store.ownerOf(cardUid) == null) throw new IllegalStateException("Unknown card");
        long bal = store.balance(cardUid);
        if (bal < Fares.BASE) throw new IllegalStateException(String.format("Balance too low: P%.2f, load at least P%.2f to ride", bal / 100.0, Fares.BASE / 100.0));

        BoardedInfo info = new BoardedInfo(cardUid, nameOf(cardUid), stopIdx, stops.get(stopIdx).name(), Instant.now());
        store.logBoarding(cardUid, operatorId, stopIdx, "BOARD");
        boarded.put(cardUid, info);
        return info;
    }

    public Txn tapOut(String rawUid, int stopIdx) {
        String cardUid = resolveCard(rawUid);
        BoardedInfo info = boarded.get(cardUid);
        if (info == null) throw new IllegalStateException("Card is not tapped in");
        if (stopIdx <= info.stopIdx()) throw new IllegalStateException("Alight stop must be after board stop");
        return charge(cardUid, Fares.pathKm(stops, info.stopIdx(), stopIdx), ruleOf(cardUid));
    }

    /** Passenger never tapped out: charge the whole route, no discount. */
    public Txn missedTapOut(String rawUid) {
        String cardUid = resolveCard(rawUid);
        if (!boarded.containsKey(cardUid)) throw new IllegalStateException("Card is not tapped in");
        return charge(cardUid, Fares.pathKm(stops, 0, stops.size() - 1), new RegularFare());
    }

    private Txn charge(String cardUid, double km, FareRule rule) {
        // The rider got on with at least the base fare, so a longer trip may leave a small negative balance.
        // The card then cannot tap in again until it is loaded. This is how an unpaid ride is never free.
        long maxFare = Fares.fare(Fares.pathKm(stops, 0, stops.size() - 1), new RegularFare());
        Txn t = store.pay(cardUid, operatorId, Fares.fare(km, rule), km, -maxFare);
        store.logBoarding(cardUid, operatorId, 0, "ALIGHT");
        boarded.remove(cardUid);                      // only after payment worked
        return t;
    }

    private FareRule ruleOf(String cardUid) {
        Optional<User> owner = store.findUser(store.ownerOf(cardUid));
        return owner.isPresent() && owner.get() instanceof Passenger p ? p.getFareRule() : new RegularFare();
    }
}