# Graph Report - fareX  (2026-10-06)

## Corpus Check
- 36 files · ~5,874 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 3 file(s) not represented in the graph (top: (none) 2, .css 1)

## Summary
- 218 nodes · 307 edges · 15 communities (9 shown, 6 thin omitted)
- Extraction: 97% EXTRACTED · 3% INFERRED · 0% AMBIGUOUS · INFERRED: 8 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `b3d5f5c6`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- ApiClient
- HelloGluon.java
- Model
- PassengerType
- 00-master-overview.md
- Enums
- AuthService
- CardService
- Shift Management
- Model.java
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
8. `Credit` - 8 edges
9. `AuthService` - 7 edges
10. `PayMongo` - 7 edges

## Surprising Connections (you probably didn't know these)
- `com.farex:farex-app` --depends_on--> `farex:farex-shared`  [EXTRACTED]
  farex-app/pom.xml → farex-shared/pom.xml
- `Session` --references--> `Stop`  [EXTRACTED]
  farex-app/src/main/java/farex/app/operator/TapService.java → farex-shared/src/main/java/farex/shared/Model.java
- `farex:farex-server` --depends_on--> `farex:farex-shared`  [EXTRACTED]
  farex-server/pom.xml → farex-shared/pom.xml
- `User` --references--> `PassengerType`  [EXTRACTED]
  farex-shared/src/main/java/farex/shared/Model.java → farex-shared/src/main/java/farex/shared/Enums.java
- `Load` --references--> `PaymentMethod`  [EXTRACTED]
  farex-shared/src/main/java/farex/shared/Model.java → farex-shared/src/main/java/farex/shared/Enums.java

## Import Cycles
- None detected.

## Communities (15 total, 6 thin omitted)

### Community 0 - "ApiClient"
Cohesion: 0.15
Nodes (5): ApiClient, SyncClient, Event, PayMongo, Qr

### Community 1 - "HelloGluon.java"
Cohesion: 0.11
Nodes (3): HelloGluon, FareCalculator, FareTable

### Community 2 - "Model"
Cohesion: 0.13
Nodes (10): Session, TapService, RewardsService, Model, PointsRow, Promotion, Route, Stop (+2 more)

### Community 3 - "PassengerType"
Cohesion: 0.12
Nodes (9): AdminService, ServerApp, TripService, PassengerType, PWD, REGULAR, SENIOR, STUDENT (+1 more)

### Community 4 - "00-master-overview.md"
Cohesion: 0.08
Nodes (28): FareX Master Overview, Modules, one file each, Rules that hold system wide, Account Management, Notes, App shown balance vs card balance, Card System (xPress Card), Lost card (+20 more)

### Community 6 - "Enums"
Cohesion: 0.11
Nodes (16): CardStatus, ACTIVE, FROZEN, Enums, LoadStatus, AWAITING_PAYMENT, CANCELLED, EXPIRED (+8 more)

### Community 7 - "AuthService"
Cohesion: 0.15
Nodes (9): AuthService, AccountStatus, ACTIVE, SUSPENDED, Role, ADMIN, OPERATOR, PASSENGER (+1 more)

### Community 8 - "CardService"
Cohesion: 0.11
Nodes (10): CardService, EarningsService, TxnType, CARD_LOAD, FARE_PAYMENT, OPERATOR_TRANSFER, POINTS_REDEEMED, Credit (+2 more)

### Community 14 - "FareX System"
Cohesion: 0.67
Nodes (3): com.farex:farex-app, farex:farex-server, farex:farex-shared

## Knowledge Gaps
- **46 isolated node(s):** `com.farex:farex-app`, `farex:farex-server`, `PASSENGER`, `OPERATOR`, `ADMIN` (+41 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 96 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **6 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Model` connect `Model` to `HelloGluon.java`, `PassengerType`, `Enums`, `AuthService`, `CardService`, `Model.java`?**
  _High betweenness centrality (0.150) - this node is a cross-community bridge._
- **Why does `PassengerType` connect `PassengerType` to `HelloGluon.java`, `Enums`, `AuthService`?**
  _High betweenness centrality (0.095) - this node is a cross-community bridge._
- **Why does `Enums` connect `Enums` to `CardService`, `PassengerType`, `AuthService`?**
  _High betweenness centrality (0.084) - this node is a cross-community bridge._
- **What connects `com.farex:farex-app`, `farex:farex-server`, `PASSENGER` to the rest of the system?**
  _46 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `HelloGluon.java` be split into smaller, more focused modules?**
  _Cohesion score 0.11462450592885376 - nodes in this community are weakly interconnected._
- **Should `Model` be split into smaller, more focused modules?**
  _Cohesion score 0.1341991341991342 - nodes in this community are weakly interconnected._
- **Should `PassengerType` be split into smaller, more focused modules?**
  _Cohesion score 0.12380952380952381 - nodes in this community are weakly interconnected._