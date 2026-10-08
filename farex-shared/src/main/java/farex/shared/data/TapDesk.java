package farex.shared.data;

import farex.shared.fare.*;
import farex.shared.txn.Txn;
import farex.shared.user.*;
import java.util.*;

/** Operator side tap logic. Who is on board lives in memory for this shift. */
public class TapDesk {
    private final Store store;
    private final String operatorId;
    private final List<RouteStop> stops;
    private final Map<String, Integer> boarded = new HashMap<>();

    public TapDesk(Store store, String operatorId) {
        this.store = store;
        this.operatorId = operatorId;
        this.stops = store.stops();
    }

    public List<RouteStop> stops() { return stops; }
    public int onBoard() { return boarded.size(); }

    public void tapIn(String cardUid, int stopIdx) {
        if (boarded.containsKey(cardUid)) throw new IllegalStateException("Card is already tapped in");
        boarded.put(cardUid, stopIdx);
    }

    public Txn tapOut(String cardUid, int stopIdx) {
        Integer from = boarded.get(cardUid);
        if (from == null) throw new IllegalStateException("Card is not tapped in");
        if (stopIdx <= from) throw new IllegalStateException("Alight stop must be after board stop");
        return charge(cardUid, Fares.pathKm(stops, from, stopIdx), ruleOf(cardUid));
    }

    /** Passenger never tapped out: charge the whole route, no discount. */
    public Txn missedTapOut(String cardUid) {
        if (!boarded.containsKey(cardUid)) throw new IllegalStateException("Card is not tapped in");
        return charge(cardUid, Fares.pathKm(stops, 0, stops.size() - 1), new RegularFare());
    }

    private Txn charge(String cardUid, double km, FareRule rule) {
        Txn t = store.pay(cardUid, operatorId, Fares.fare(km, rule), km);
        boarded.remove(cardUid);                      // only after payment worked
        return t;
    }

    private FareRule ruleOf(String cardUid) {
        Optional<User> owner = store.findUser(store.ownerOf(cardUid));
        return owner.isPresent() && owner.get() instanceof Passenger p ? p.getFareRule() : new RegularFare();
    }
}
