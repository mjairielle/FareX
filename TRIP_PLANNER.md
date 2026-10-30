# FareX — Trip Planner Handoff

Owner: Kenn. Scope of this half: **routing algorithm, graph, CSV loading, and the route data itself.**
Not in scope: the UI, the tap/ticketing side, `FareCalculator`.

Everything below compiles under `javac 21`+ with no warnings under `-Xlint:all` and no external
dependencies. Both test suites pass.

---

## 1. Run it

From the repo root (sources are at the root in `package FareX`, tests in `test/`):

```bash
javac -Xlint:all -encoding UTF-8 -d out *.java test/*.java
java -cp out FareX.PlannerTest     # 29 assertions on a hand-built Las Piñas/Makati network
java -cp out FareX.StressTest      # 300 random networks, 2244 queries, vs BFS + exhaustive fare search
java -cp out FareX.LoaderTest      # loads data/*.csv and plans a real trip
```

Last run (2026-10-30, JDK 26): `PASSED 29  FAILED 0`, `STRESS PASS`, 0 of 2212 queries had a
cheaper trip than the planner found, slowest `findPaths` call ~14 ms.

---

## 2. The one design decision that must not be broken

**A graph node is a `(stop, route)` pair, not a stop.**

Node key format is `STOP_ID@ROUTE_ID` — e.g. `S04@J01` and `S04@J02` are Alabang on two
different routes. They are separate nodes joined by a `TRANSFER` edge.

If you ever "simplify" this so a node is just a stop, the algorithm will think you can change
jeeps for free and every result becomes nonsense. This is the single most important invariant
in the whole planner.

Helpers: `RouteGraph.nodeKey()`, `.stopIdOf()`, `.routeIdOf()`.

---

## 3. How fare is handled (read before touching the weights)

The fare is `base + perKm * (km - baseKm)`, rounded to the nearest ₱0.25, charged **once per
boarding**. That is not additive per segment — the cost of one more stop depends on how far
you've already ridden on that vehicle — and Dijkstra needs additive weights.

**The trap (what an earlier version did):** charge `perKm * km` on every ride segment and
`baseFare` at each boarding. That *is* additive, but it overcharges each leg by
`perKm * min(km, baseKm)`. That amount is **not** the same for every candidate: a bus leg at
₱2.98/km is overcharged more than a jeep leg at ₱2.40/km, and a short leg inside the base band
less than a long one. So it could rank the wrong trip as cheapest. Measured: on the stress test's
2212 reachable queries, that version missed a cheaper trip **86 times (~4%)**.

**What the code does now** (`RouteGraph.cheapestByFare`): one Dijkstra step is a whole **leg**,
not a segment. A leg's fare depends only on where you board and alight, so it is priced exactly
with `Route.fareFor()`, and a journey's fare is just the sum of its legs — additive again, so
Dijkstra is exact. State is still a `(stop, route)` node: `S04@J01` means "standing at S04, just
got off J01"; from there you may board any *other* route at S04. The origin is the virtual node
`S01@`. The TIME and TRANSFERS passes are additive per segment already and use the ride/transfer
edges as before.

`StressTest` proves it: for every query it runs an independent exhaustive search (every journey up
to 4 legs) and fails if that ever finds something cheaper than the planner. Result: 0 of 2212.

This is worth being able to explain in the defense — it's the most interesting thing in the code.

---

## 4. Files

| File | What it is | Status |
|---|---|---|
| `Stop.java` | stop + haversine distance | done |
| `Route.java` | ordered stops, fare params, speed | done |
| `Leg.java` | one boarding→alighting ride | done |
| `TripOption.java` | a full journey, totals computed at construction | done |
| `RouteGraph.java` | graph build + Dijkstra + path reconstruction | done |
| `TripPlanner.java` | dedupe, rank, cap at 3, affordability | done |
| `RouteLoader.java` | reads the three CSVs | done |
| `Vehicle.java` | the group's own class, unchanged — `Route` only needs it to exist | group's |
| `test/PlannerTest.java` / `StressTest.java` / `LoaderTest.java` | **do not submit** — delete `test/` before handing in | tests |
| `data/*.csv` | the network the loader reads | invented routes, real fares |

`Leg.java` did not exist in the original skeleton. `TripOption` held `List<Route>`, which names
*which* routes but not *where* you board and alight — so distance and fare were uncomputable.
That was the blocking bug.

---

## 5. CSV format

Column order is read from each **header row**, not assumed by position. Reordering a column is
safe; renaming one is not.

```
data/stops.csv        stop_id,name,lat,lng
data/routes.csv       route_id,name,base_fare,base_km,per_km_rate,avg_speed_kph,active
data/route_stops.csv  route_id,stop_id,sequence
```

`route_stops.csv` is the important one — `sequence` defines the order along the route, and rows
are sorted by it, so the row order in the file doesn't matter. Verified with a shuffled file.

The loader handles, without crashing: UTF-8 BOM from Excel, quoted fields containing commas,
`#` comments, blank lines, unknown route/stop references, non-numeric values, and wrong column
counts. Everything it skips is reported in `getWarnings()` — **print those during the demo**,
an empty warnings list is a real signal the data is clean.

---

## 6. Next up, in priority order

### 6a. Wire in `FareCalculator` (decided: FareCalculator owns the formula)

Right now the formula (including the ₱0.25 rounding) lives in `Route.fareFor(double km)`. It is
called from two places: the `Leg` constructor (displayed fare) and `RouteGraph.cheapestByFare`
(the search). Both must use the same formula, or the search optimises a different fare than the
one shown.

In the repo, `FareCalculator` is still a stub: `compute(double distanceKm, PassengerType type,
FareTable ft)` returns `0.0`, and `FareTable` has `baseFare` and `ratePerKm` but **no `baseKm`**
(4 km for jeeps, 5 km for buses), no getters and no constructor. To finish this:

1. Add `baseKm` to `FareTable`, plus a constructor and getters.
2. Implement `FareCalculator.compute` with the same formula and rounding as `Route.fareFor`.
3. Have `Route.fareFor` delegate to it (a `Route` holds or builds its `FareTable`), so `Leg` and
   the search both go through `FareCalculator` with no other changes.
4. `addRideEdges`' `perKmRate * km` is informational only now (§3) — not a copy of the formula.

Two copies of a fare formula that silently drift apart is exactly the kind of bug that shows up
during a defense.

### 6b. Discounts (student / senior / PWD)

20% statutory discount. `PassengerType.java` already exists in the repo — use it rather than a
new enum. Apply it in the same place as 6a so there's one fare path, not two.

Do **not** apply the discount to the search weights. Discounting every candidate by the same
percentage can't change their order, so it only belongs in the displayed fare.

The LTFRB prints the discounted fare as its own column, discounting the *scale* first and
rounding afterwards: a ₱14.00 jeep minimum becomes `14.00 × 0.8 = 11.20`, then rounds to ₱11.25.
So do `roundToQuarter(0.8 × rawFare)`, **not** `0.8 × roundedFare`. The fare-matrix page gives
reference values to test against (e.g. traditional jeep 20 km: ₱46.00 regular, ₱36.75 student).

### 6c. Affordability check

`TripPlanner.shortfall(option, balance)` already exists and is tested. It needs wiring to the
real card balance — `XPressCard` in the repo. This is the feature that makes FareX one system
instead of two projects stapled together, so it's worth doing properly.

### 6d. Service hours

Add `start_time,end_time` columns to `routes.csv`, filter in `RouteGraph.build()` the same way
`isActive()` is filtered today. A 2 AM query should return "no routes at this time", not a
fake answer.

### 6e. Real Metro Manila route data

Current `data/*.csv` is 4 invented routes for testing. Replace with real ones.

Best source is **OpenStreetMap**, filtered on `network=PUJ` — actively maintained, covers modern
and traditional jeepney routes. Overpass query, bounding box around Metro Manila, convert to the
three CSVs above. There's also a GTFS feed at `github.com/sakayph/gtfs` (GTFS files *are* CSV),
but it's from the 2013 transit app challenge so it predates PUV modernization.

**Seed offline.** Run the fetch once, write the CSVs, commit them. Don't make a live API call
part of the running app — campus wifi during a defense is not a risk worth taking.

8–12 routes you can personally verify beats 500 you can't.

---

## 7. Known gaps — say these out loud in the defense rather than hoping nobody asks

**Fare *rates* are real, distances are not.** `data/routes.csv` uses the LTFRB scales effective
28 Sep 2026 (modern jeepney ₱17.00 / first 4 km + ₱2.40/km; aircon city bus ₱18.00 / first 5 km
+ ₱2.98/km), rounded to the nearest ₱0.25 the way the guides do. Source: the transcribed matrix
at parkinganph.com/guides/fare-matrix (checked 2026-10-30); `PlannerTest` §8 checks our formula
against its printed values. The *distances* are still straight-line × 1.3, so a real trip's
fare is only as good as that estimate. Zapote→Ayala now shows ₱48.50 direct. Ride one route,
compare the km and the fare you actually pay, and tune `ROAD_WINDING_FACTOR`.

**All routes are treated as bidirectional.** `addRideEdges` builds edges both ways. Plenty of real
jeepney routes are one-way loops. Fix by adding a `loop` column and building forward-only edges
for those.

**A route cannot visit the same stop twice.** `Route` keys positions by stop ID (`indexOf` returns
the first match), so a repeated stop would give wrong distances. The constructor now throws on it
and `RouteLoader` skips the repeat with a warning. Real loop support needs nodes keyed by
position along the route, not by stop ID.

**Distance is straight-line × 1.3.** `Stop.ROAD_WINDING_FACTOR`. Real road distance would need a
routing API — if you do that, cache results into a CSV, don't call it live.

**No walking transfers.** You can only change routes at a stop both routes share. In reality you'd
walk 200 m between two nearby stops. `RouteGraph.stopsWithinWalk()` exists and is unused — the
hook is there if you want it.

**Stops are a modelling fiction.** Jeepney drivers stop wherever a passenger flags them, and cut
routes short when there aren't enough riders. No dataset matches reality here. Acknowledging this
reads as rigor, not weakness.

---

## 8. Constants, all in one place each

| Constant | File | Current | Notes |
|---|---|---|---|
| `ROAD_WINDING_FACTOR` | `Stop` | 1.3 | straight-line → road correction |
| `DEFAULT_TRANSFER_MINUTES` | `TripOption` | 5.0 | default wait + walk per transfer |
| `transferMinutes` | `RouteGraph` | 5.0 | settable before `build()`; used by the search **and** passed to every `TripOption`, so they can't drift |
| fare rounding | `Route.roundToQuarter` | ₱0.25 | LTFRB nearest-25-centavo rule, half up |
| transfer weight | `RouteGraph.weightOf` | 1000.0 | large constant for FEWEST_TRANSFERS |
| `MAX_OPTIONS` | `TripPlanner` | 3 | how many results the user sees |
| `avg_speed_kph` | `data/routes.csv` | 12–40 | 15 km/h is realistic for Manila traffic |

---

## 9. Still open

- The repo says `XPressCard`, the project is called FareX. Pick one before submission — keep it
  out of class names either way so renaming stays a one-line change.
- J03 in `data/` runs Southmall → Sucat → ATC, which doubles back north. Fine for testing, not
  real geography; replace when 6e lands.
- UI is someone else's. The planner's entry points are `TripPlanner.plan(origin, destination)`
  and `planAllPreferences(origin, destination)`; both return `List<TripOption>`, and
  `TripOption.toDetailedString()` gives a ready-made per-leg breakdown to print.
