package farex.shared.plan;

import farex.shared.data.RouteBook;
import farex.shared.data.RouteStop;
import farex.shared.fare.FareRule;
import farex.shared.fare.Fares;
import java.util.*;

/**
 * Best routes from A to B. Graph nodes are (route, stop index). Edges are riding one stop
 * and switching routes at a stop with the same name. One Dijkstra run per ranking, only the cost differs.
 * shortcut: places match by exact name, no walking between nearby stops. Upgrade when real stop data exists.
 */
public final class TripPlanner {
    public record Leg(String route, String from, String to, double km) {}
    public record Plan(String label, List<Leg> legs, double km, int transfers, long fareCents, int minutes) {}

    private record Node(String route, int idx) {}
    private record Entry(Node node, double cost) {}

    /** cost of boarding a jeepney, cost of riding one km */
    private record Cost(String label, double board, double perKm) {}

    private final Map<String, List<RouteStop>> routes;

    public TripPlanner(RouteBook book) { this.routes = book.routes(); }

    /** At most 3 plans. A plan that wins several rankings appears once with joined labels. */
    public List<Plan> plan(String from, String to, FareRule rule) {
        if (from.equals(to)) throw new IllegalArgumentException("Pick two different places");
        boolean hasFrom = false, hasTo = false;
        for (List<RouteStop> l : routes.values()) for (RouteStop s : l) { hasFrom |= s.name().equals(from); hasTo |= s.name().equals(to); }
        if (!hasFrom || !hasTo) throw new IllegalArgumentException("Unknown place");

        double keep = (100 - rule.discountPercent()) / 100.0;
        Cost[] costs = {
            new Cost("Shortest", 0, 1),
            new Cost("Fewest transfers", 1000, 1),
            new Cost("Cheapest", Fares.BASE * keep, Fares.RATE_PER_KM * keep)};

        Map<String, Plan> bySignature = new LinkedHashMap<>();
        for (Cost c : costs) {
            List<Leg> legs = search(from, to, c);
            if (legs == null) continue;
            String sig = legs.toString();
            Plan old = bySignature.get(sig);
            bySignature.put(sig, old == null ? build(c.label(), legs, rule) : relabel(old, old.label() + " + " + c.label()));
        }
        return new ArrayList<>(bySignature.values());
    }

    private Plan build(String label, List<Leg> legs, FareRule rule) {
        double km = 0;
        long fare = 0;
        for (Leg l : legs) { km += l.km(); fare += Fares.fare(l.km(), rule); }
        int transfers = legs.size() - 1;
        return new Plan(label, legs, km, transfers, fare, (int) Math.round(km / 15.0 * 60) + transfers * 5);
    }

    private Plan relabel(Plan p, String label) { return new Plan(label, p.legs(), p.km(), p.transfers(), p.fareCents(), p.minutes()); }

    private double segKm(String route, int a, int b) { return Fares.pathKm(routes.get(route), Math.min(a, b), Math.max(a, b)); }

    private String name(Node n) { return routes.get(n.route()).get(n.idx()).name(); }

    /** Dijkstra. Returns legs of the cheapest way, null when places are not connected. */
    private List<Leg> search(String from, String to, Cost c) {
        Map<Node, Double> dist = new HashMap<>();
        Map<Node, Node> prev = new HashMap<>();
        PriorityQueue<Entry> pq = new PriorityQueue<>(Comparator.comparingDouble(Entry::cost));
        for (var e : routes.entrySet())
            for (int i = 0; i < e.getValue().size(); i++)
                if (e.getValue().get(i).name().equals(from)) { Node n = new Node(e.getKey(), i); dist.put(n, c.board()); pq.add(new Entry(n, c.board())); }

        while (!pq.isEmpty()) {
            Entry cur = pq.poll();
            if (cur.cost() > dist.getOrDefault(cur.node(), Double.MAX_VALUE)) continue;
            Node n = cur.node();
            if (name(n).equals(to)) return legsOf(n, prev);
            List<RouteStop> line = routes.get(n.route());
            for (int d : new int[]{-1, 1}) {                       // ride one stop, either direction
                int j = n.idx() + d;
                if (j < 0 || j >= line.size()) continue;
                double km = segKm(n.route(), n.idx(), j);
                relax(new Node(n.route(), j), n, cur.cost() + c.perKm() * km + km * 1e-6, dist, prev, pq);
            }
            for (var e : routes.entrySet()) {                      // switch jeepney at a same name stop
                if (e.getKey().equals(n.route())) continue;
                // + 1e-3: when two routes share a road, riding on and switching tie on km; without a
                // tie-breaker "Shortest" could suggest a pointless transfer (and a second fare)
                for (int j = 0; j < e.getValue().size(); j++)
                    if (e.getValue().get(j).name().equals(name(n)))
                        relax(new Node(e.getKey(), j), n, cur.cost() + c.board() + 1e-3, dist, prev, pq);
            }
        }
        return null;
    }

    private void relax(Node to, Node from, double cost, Map<Node, Double> dist, Map<Node, Node> prev, PriorityQueue<Entry> pq) {
        if (cost < dist.getOrDefault(to, Double.MAX_VALUE)) { dist.put(to, cost); prev.put(to, from); pq.add(new Entry(to, cost)); }
    }

    private List<Leg> legsOf(Node end, Map<Node, Node> prev) {
        LinkedList<Node> path = new LinkedList<>();
        for (Node n = end; n != null; n = prev.get(n)) path.addFirst(n);
        List<Leg> legs = new ArrayList<>();
        int start = 0;
        for (int i = 1; i <= path.size(); i++) {
            if (i == path.size() || !path.get(i).route().equals(path.get(start).route())) {
                Node a = path.get(start), b = path.get(i - 1);
                if (a.idx() != b.idx()) legs.add(new Leg(a.route(), name(a), name(b), segKm(a.route(), a.idx(), b.idx())));
                start = i;
            }
        }
        return legs;
    }
}
