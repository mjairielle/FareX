# Trip Planner

Back to [Master Overview](00-master-overview.md).

```mermaid
flowchart TD
    PHOME[Passenger Home] --> TPSCREEN[Trip Planner]
    TPSCREEN --> INPUTPOINTS["Enter Point A, Point B"]
    INPUTPOINTS --> SEARCH[Search Route Graph, built from Transport System data]
    SEARCH --> FOUND{Any Route Found?}
    FOUND -->|No| NOROUTE["No Route Found, suggest nearby stops"]
    FOUND -->|Yes| RANK[Rank Candidate Routes]

    RANK --> R1["Route 1, shortest distance"]
    RANK --> R2["Route 2, fewest transfers"]
    RANK --> R3["Route 3, cheapest fare"]

    R1 --> SHOW[Show up to 3 Routes]
    R2 --> SHOW
    R3 --> SHOW

    SHOW --> PICK[Passenger Picks One]
    PICK --> DETAIL["Route Detail: jeepney to board, stops, estimated fare, estimated time"]
    DETAIL --> STARTNAV[Start Trip Tracking, optional]
```

## Ranking logic

Route graph nodes are stops, edges are jeepney legs pulled from the Transport System's route table. Each edge carries distance and fare (base fare + rate per km, minus discount if applicable).

- Shortest distance, sum edge distances, pick lowest.
- Fewest transfers, count number of jeepney changes, pick lowest.
- Cheapest fare, sum edge fares, pick lowest.

If two candidates tie on the criterion, break the tie with total distance. Duplicate routes across criteria collapse into one card, so passenger never sees the same route listed twice even if it wins on more than one criterion.

Next: [Transport System](03-transport-system.md).
