# Graph Report - FareX  (2026-11-19)

## Corpus Check
- 167 files · ~63,252 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 18 file(s) not represented in the graph (top: .csv 11, (none) 4, .bat 2)

## Summary
- 1720 nodes · 6751 edges · 68 communities (32 shown, 36 thin omitted)
- Extraction: 90% EXTRACTED · 10% INFERRED · 0% AMBIGUOUS · INFERRED: 676 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `e83367b6`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- javafx.scene.layout.VBox
- Api
- OtpView
- .panel
- .bold
- FetchRouteShapes.java
- OperatorState
- .main
- TripScreen
- org.junit.jupiter.api.Test
- TapRider
- PopupDialog
- 00-master-overview.md
- LostCardScreen
- Txn
- Main.java
- User
- CardPayload.java
- DemoAccount
- DemoCard
- .editStep
- TapDesk
- ShiftRecord
- TapTerminal.java
- .view
- .bold
- StepScreen
- TapResult
- .chooseStep
- .small
- server
- BuyLoadState
- Rewards
- ProgressStep
- DemoNotification
- CardNumberField
- MainShell
- ShiftSession
- CardChoiceTile
- DemoTransaction
- TapResultDialog
- TripPlanner
- .getCurrentAccount
- .LoadReceiptStep
- javafx.scene.Node
- OperatorHistoryView
- AmountField
- OperatorUi
- FetchLoadState
- javafx.scene.layout.Pane
- .TapPreviewDialog
- ClassicNfc
- OfflineQueue
- Passenger
- FareX
- ChangePhotoSheet
- FarexApp
- .get
- Operator
- OperatorShell
- test.sh
- .getCards
- AdminPage
- UserDemo
- com.farex:farex-shared
- farex-server/README.md
- com.farex:farex-parent

## God Nodes (most connected - your core abstractions)
1. `DemoCard` - 82 edges
2. `Api` - 70 edges
3. `Theme` - 64 edges
4. `DemoAccount` - 61 edges
5. `OperatorState` - 55 edges
6. `Navigator` - 51 edges
7. `CommonUi` - 50 edges
8. `PopupDialog` - 43 edges
9. `MockData` - 39 edges
10. `LeafletMap` - 38 edges

## Surprising Connections (you probably didn't know these)
- `TapRider` --references--> `Passenger`  [EXTRACTED]
  farex-app/src/main/java/com/farex/ui/operator/TapRider.java → farex-shared/src/main/java/farex/shared/user/Passenger.java
- `Main` --references--> `RouteBook`  [EXTRACTED]
  farex-server/src/main/java/farex/server/Main.java → farex-shared/src/main/java/farex/shared/data/RouteBook.java
- `Main` --references--> `RouteShapes`  [EXTRACTED]
  farex-server/src/main/java/farex/server/Main.java → farex-shared/src/main/java/farex/shared/data/RouteShapes.java
- `Main` --references--> `Store`  [EXTRACTED]
  farex-server/src/main/java/farex/server/Main.java → farex-shared/src/main/java/farex/shared/data/Store.java
- `Main` --references--> `TapDesk`  [EXTRACTED]
  farex-server/src/main/java/farex/server/Main.java → farex-shared/src/main/java/farex/shared/data/TapDesk.java

## Import Cycles
- None detected.

## Communities (68 total, 36 thin omitted)

### Community 0 - "javafx.scene.layout.VBox"
Cohesion: 0.06
Nodes (20): AccountTypeView, RoleCard, CongratsView, SplashView, WelcomeView, Assets, Avatar, CardPickerDialog (+12 more)

### Community 1 - "Api"
Cohesion: 0.05
Nodes (16): Api, ApiException, BoardedPassenger, FareRuleInfo, PlanCard, PlanRide, Points, RouteDetail (+8 more)

### Community 2 - "OtpView"
Cohesion: 0.06
Nodes (8): AuthenticationScreen, LoginView, RegisterView, SignUpState, IconField, LabeledField, OtpView, ChangePasswordView

### Community 3 - ".panel"
Cohesion: 0.10
Nodes (3): FetchLoadScreen, OperatorIntroView, TermsView

### Community 4 - ".bold"
Cohesion: 0.10
Nodes (3): ProfileView, LocationPermissionDialog, RoutesView

### Community 5 - "FetchRouteShapes.java"
Cohesion: 0.07
Nodes (7): FetchRouteShapes, Road, OperatorDemo, RoadDistances, RouteBook, RouteShapes, RoadKmTest

### Community 6 - "OperatorState"
Cohesion: 0.07
Nodes (3): StopCoord, GpsSimulator, OperatorState

### Community 8 - "TripScreen"
Cohesion: 0.09
Nodes (3): TripPlace, TripScreen, TripState

### Community 9 - "org.junit.jupiter.api.Test"
Cohesion: 0.11
Nodes (8): SampleShift, DiscountedFare, FareRule, FareRuleDemo, RegularFare, TripPlannerDemo, FareRuleTest, TripPlannerTest

### Community 10 - "TapRider"
Cohesion: 0.12
Nodes (4): TapPickerDialog, TapRider, TapTerminal, Boarding

### Community 12 - "00-master-overview.md"
Cohesion: 0.08
Nodes (28): FareX Master Overview, Modules, one file each, Rules that hold system wide, Account Management, Notes, App shown balance vs card balance, Card System (xPress Card), Lost card (+20 more)

### Community 13 - "LostCardScreen"
Cohesion: 0.09
Nodes (5): LostCardScreen, FreezeStep, OtpStep, TransferStep, LostCardState

### Community 14 - "Txn"
Cohesion: 0.10
Nodes (4): CardLoad, FarePayment, Txn, TxnTest

### Community 15 - "Main.java"
Cohesion: 0.09
Nodes (4): Env, Csv, StoreDemo, TxnDemo

### Community 16 - "User"
Cohesion: 0.11
Nodes (5): Main, Act, Admin, User, UserTest

### Community 17 - "CardPayload.java"
Cohesion: 0.10
Nodes (5): CardIO, CardPayload, Data, SimCardIO, CardPayloadTest

### Community 19 - "DemoCard"
Cohesion: 0.15
Nodes (3): CardTile, DemoCard, PendingLoad

### Community 20 - ".editStep"
Cohesion: 0.12
Nodes (4): ToggleSwitch, PrivacyView, LocationOptions, ProfileState

### Community 21 - "TapDesk"
Cohesion: 0.16
Nodes (3): TapDesk, BoardedInfo, OperatorTest

### Community 25 - ".bold"
Cohesion: 0.18
Nodes (4): EndShiftDialog, OperatorSummaryView, OperatorSyncScreen, UploadStep

### Community 27 - "TapResult"
Cohesion: 0.13
Nodes (11): TapResult, Kind, APP_OFFLINE, APP_TAP_IN, BLOCKED, INSUFFICIENT, INVALID_QR, MAX_FARE (+3 more)

### Community 28 - ".chooseStep"
Cohesion: 0.17
Nodes (3): RedeemScreen, ProgressStepView, RedeemState

### Community 30 - "server"
Cohesion: 0.10
Nodes (19): context, dockerfile, path, required, interval, retries, start_period, test (+11 more)

### Community 31 - "BuyLoadState"
Cohesion: 0.15
Nodes (4): BuyLoadState, LoadAccountStep, LoadMethodStep, LoadReviewStep

### Community 32 - "Rewards"
Cohesion: 0.22
Nodes (5): Rewards, Perk, Promo, RewardsDemo, RewardsTest

### Community 35 - "CardNumberField"
Cohesion: 0.22
Nodes (3): CardNumberField, AddCardView, LinkStep

### Community 41 - "TripPlanner"
Cohesion: 0.44
Nodes (6): TripPlanner, Cost, Entry, Leg, Node, Plan

### Community 45 - "OperatorHistoryView"
Cohesion: 0.24
Nodes (6): OperatorHistoryView, Filter, ALL, APP, CARD, PENDING

### Community 54 - "FareX"
Cohesion: 0.25
Nodes (7): Configuration & Environment Variables, Connecting the Client App, FareX, Prerequisites, Quick Start with Docker Compose, Running with Docker (Backend Server), Running with Plain Docker CLI

### Community 56 - "FarexApp"
Cohesion: 0.29
Nodes (3): FarexApp, TripMapExploreView, UiLauncher

### Community 60 - "test.sh"
Cohesion: 0.53
Nodes (4): H(), j(), ok(), test.sh script

### Community 64 - "com.farex:farex-shared"
Cohesion: 0.67
Nodes (3): com.farex:farex-app, com.farex:farex-server, com.farex:farex-shared

## Knowledge Gaps
- **16 isolated node(s):** `com.farex:farex-app`, `ALL`, `CARD`, `APP`, `PENDING` (+11 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 209 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **36 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `DemoCard` connect `DemoCard` to `javafx.scene.layout.VBox`, `CardNumberField`, `.bold`, `.panel`, `CardChoiceTile`, `.getCurrentAccount`, `PopupDialog`, `TapRider`, `LostCardScreen`, `javafx.scene.Node`, `FetchLoadState`, `DemoAccount`, `.TapPreviewDialog`, `Passenger`, `TapTerminal.java`, `.chooseStep`, `.getCards`, `BuyLoadState`?**
  _High betweenness centrality (0.072) - this node is a cross-community bridge._
- **What connects `com.farex:farex-app`, `ALL`, `CARD` to the rest of the system?**
  _16 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `javafx.scene.layout.VBox` be split into smaller, more focused modules?**
  _Cohesion score 0.0583677482792527 - nodes in this community are weakly interconnected._
- **Why does `Api` connect `Api` to `javafx.scene.layout.VBox`, `OperatorState`, `TapTerminal.java`?**
  _High betweenness centrality (0.066) - this node is a cross-community bridge._
- **Should `Api` be split into smaller, more focused modules?**
  _Cohesion score 0.05078416728902166 - nodes in this community are weakly interconnected._
- **Why does `RegularFare` connect `org.junit.jupiter.api.Test` to `javafx.scene.layout.VBox`, `FetchRouteShapes.java`, `.main`, `TapRider`, `TapDesk`, `TapTerminal.java`, `TapResult`?**
  _High betweenness centrality (0.052) - this node is a cross-community bridge._
- **Should `OtpView` be split into smaller, more focused modules?**
  _Cohesion score 0.05803571428571429 - nodes in this community are weakly interconnected._