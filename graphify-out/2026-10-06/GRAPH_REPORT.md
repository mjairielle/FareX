# Graph Report - fareX  (2026-10-04)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 201 nodes · 246 edges · 40 communities (9 shown, 31 thin omitted)
- Extraction: 97% EXTRACTED · 3% INFERRED · 0% AMBIGUOUS · INFERRED: 8 edges (avg confidence: 0.85)
- Token cost: 827 input · 1,705 output

## Graph Freshness
- Built from commit: `b3d5f5c6`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- API Payment
- Frontend
- Fare Calculation
- Admin System
- Promotion System
- Card Handling
- Account Status
- User Management
- Transaction System
- Shift Management
- CSV Handling
- Security Signer
- GPS Tracking
- FareX System
- FareX Backend

## God Nodes (most connected - your core abstractions)
1. `Model` - 15 edges
2. `PassengerType` - 14 edges
3. `CardService` - 11 edges
4. `AdminService` - 10 edges
5. `ApiClient` - 9 edges
6. `Enums` - 9 edges
7. `TxnType` - 8 edges
8. `PayMongo` - 7 edges
9. `AuthService` - 7 edges
10. `User` - 7 edges

## Surprising Connections (you probably didn't know these)
- `Session` --references--> `Stop`  [EXTRACTED]
  farex-app/src/main/java/farex/app/operator/TapService.java → farex-shared/src/main/java/farex/shared/Model.java
- `com.farex:farex-app` --depends_on--> `farex:farex-shared`  [EXTRACTED]
  farex-app/pom.xml → farex-shared/pom.xml
- `farex:farex-server` --depends_on--> `farex:farex-shared`  [EXTRACTED]
  farex-server/pom.xml → farex-shared/pom.xml
- `Txn` --references--> `TxnType`  [EXTRACTED]
  farex-shared/src/main/java/farex/shared/Model.java → farex-shared/src/main/java/farex/shared/Enums.java
- `User` --references--> `PassengerType`  [EXTRACTED]
  farex-shared/src/main/java/farex/shared/Model.java → farex-shared/src/main/java/farex/shared/Enums.java

## Import Cycles
- None detected.

## Communities (40 total, 31 thin omitted)

### Community 0 - "API Payment"
Cohesion: 0.15
Nodes (5): ApiClient, SyncClient, Event, PayMongo, Qr

### Community 2 - "Fare Calculation"
Cohesion: 0.20
Nodes (9): Session, TapService, FareCalculator, Model, PointsRow, Route, Stop, Txn (+1 more)

### Community 3 - "Admin System"
Cohesion: 0.15
Nodes (8): AdminService, TripService, PassengerType, PWD, REGULAR, SENIOR, STUDENT, TripOption

### Community 4 - "Promotion System"
Cohesion: 0.12
Nodes (4): RewardsService, ServerApp, SyncService, Promotion

### Community 5 - "Card Handling"
Cohesion: 0.14
Nodes (8): CardService, LoadStatus, AWAITING_PAYMENT, EXPIRED, FAILED, PAID, Load, SignedBalance

### Community 6 - "Account Status"
Cohesion: 0.13
Nodes (13): AccountStatus, ACTIVE, SUSPENDED, CardStatus, ACTIVE, FROZEN, Enums, PaymentMethod (+5 more)

### Community 7 - "User Management"
Cohesion: 0.20
Nodes (6): AuthService, Role, ADMIN, OPERATOR, PASSENGER, User

### Community 8 - "Transaction System"
Cohesion: 0.20
Nodes (7): EarningsService, TxnType, CARD_LOAD, FARE_PAYMENT, OPERATOR_TRANSFER, POINTS_REDEEMED, Credit

### Community 14 - "FareX System"
Cohesion: 0.67
Nodes (3): com.farex:farex-app, farex:farex-server, farex:farex-shared

## Knowledge Gaps
- **28 isolated node(s):** `PointsRow`, `Vehicle`, `com.farex:farex-app`, `farex:farex-server`, `PWD` (+23 more)
  These have ≤1 connection - possible missing edges. (Counts symbols only; 101 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **31 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Model` connect `Fare Calculation` to `Frontend`, `Admin System`, `Promotion System`, `Card Handling`, `Account Status`, `User Management`, `Transaction System`, `Transportation Tap`?**
  _High betweenness centrality (0.176) - this node is a cross-community bridge._
- **Why does `PassengerType` connect `Admin System` to `Frontend`, `User Management`, `Account Status`, `Transportation Tap`?**
  _High betweenness centrality (0.111) - this node is a cross-community bridge._
- **Why does `Enums` connect `Account Status` to `Transaction System`, `Admin System`, `Card Handling`, `User Management`?**
  _High betweenness centrality (0.100) - this node is a cross-community bridge._
- **What connects `PointsRow`, `Vehicle`, `com.farex:farex-app` to the rest of the system?**
  _28 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Frontend` be split into smaller, more focused modules?**
  _Cohesion score 0.14705882352941177 - nodes in this community are weakly interconnected._
- **Should `Promotion System` be split into smaller, more focused modules?**
  _Cohesion score 0.125 - nodes in this community are weakly interconnected._
- **Should `Card Handling` be split into smaller, more focused modules?**
  _Cohesion score 0.14285714285714285 - nodes in this community are weakly interconnected._