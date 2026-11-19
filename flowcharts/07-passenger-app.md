# Passenger App

Back to [Master Overview](00-master-overview.md).

```mermaid
flowchart TD
    LOGIN[Login Success] --> PDASH[Passenger Dashboard]
    PDASH --> HOME[Home Page]
    HOME --> XCARD["xPress Card, see Card System"]
    HOME --> ROUTES[Routes]
    HOME --> TXN["Transactions, see Transactions"]
    HOME --> PROFILE[Profile]
    HOME --> TRIP["Trip Planner, see Trip Planner"]

    ROUTES --> ROUTELIST[Browse or Search Routes]
    ROUTELIST --> ROUTEVIEW[View Stops, Fare Table, Active Jeepneys on Route]

    PROFILE --> PVIEW["View Info: name, contact, email, passenger type"]
    PVIEW --> PEDIT[Edit Info]
    PEDIT --> PSAVE[Save, Syncs to Server]
    PROFILE --> PCARDS[Linked Cards List]
    PROFILE --> PLOGOUT[Log Out]
```

## Screen map

| Screen | Feeds into |
|---|---|
| xPress Card | [Card System](02-card-system.md) |
| Routes | [Transport System](03-transport-system.md) |
| Trip Planner | [Trip Planner](04-trip-planner.md) |
| Transactions | [Transactions](06-transactions.md) |
| Profile | Account Management data, editable subset |

Profile shows passenger type (regular, student, senior, PWD), this is what the fare engine reads for the 20% discount check.

Next: [Card System](02-card-system.md).
