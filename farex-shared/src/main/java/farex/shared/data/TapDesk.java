package farex.shared.data;

import farex.shared.fare.*;
import farex.shared.txn.Txn;
import farex.shared.user.*;
import java.time.Instant;
import java.util.*;

/** Operator side tap logic. Who is on board lives in memory for this shift. */
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
    }

    public List<RouteStop> stops() { return stops; }
    public int onBoard() { return boarded.size(); }
    public List<BoardedInfo> boardedPassengers() { return new ArrayList<>(boarded.values()); }
    public void clearBoarded() { boarded.clear(); }

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
        if (boarded.containsKey(cardUid)) throw new IllegalStateException("Card is already tapped in");
        if (stopIdx < 0 || stopIdx >= stops.size()) throw new IllegalArgumentException("Invalid stop index");

        String ownerId = store.ownerOf(cardUid);
        String name = store.findUser(ownerId).map(User::getName).orElse("Card " + Store.formatUid(cardUid));
        BoardedInfo info = new BoardedInfo(cardUid, name, stopIdx, stops.get(stopIdx).name(), Instant.now());
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
        Txn t = store.pay(cardUid, operatorId, Fares.fare(km, rule), km);
        boarded.remove(cardUid);                      // only after payment worked
        return t;
    }

    private FareRule ruleOf(String cardUid) {
        Optional<User> owner = store.findUser(store.ownerOf(cardUid));
        return owner.isPresent() && owner.get() instanceof Passenger p ? p.getFareRule() : new RegularFare();
    }
}