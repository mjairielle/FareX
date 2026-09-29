# Transport System

Back to [Master Overview](00-master-overview.md).

```mermaid
flowchart TD
    OPHONE[Operator Phone, GPS On] --> TRACK[Track Jeepney Position While On Route]
    TRACK --> DETECT{Near a Known Stop?}
    DETECT -->|Yes| LOGSTOP[Log Stop Arrival, Timestamp, Coordinates]
    DETECT -->|No| TRACK
    LOGSTOP --> LOCALSTOPCSV[(Local Stops CSV)]
    LOCALSTOPCSV --> SYNC[Synced to Server on Next Upload]
    SYNC --> ADMINREVIEW{Admin Reviews New Stop?}
    ADMINREVIEW -->|Approve| ROUTEDB[(Master Routes and Stations CSV)]
    ADMINREVIEW -->|Reject| DISCARD[Discard Point, Likely GPS Noise]

    ROUTEDB --> ROUTESCREEN[Passenger Routes Screen]
    ROUTESCREEN --> ROUTELIST[List of Jeepney Routes]
    ROUTELIST --> ROUTEDETAIL[Route Detail: Stops in Order, Fare Table, Active Vehicles]

    ROUTEDB --> FAREENGINE[Fare Calculation]
    FAREENGINE --> FORMULA["Fare = Base Fare + Rate per KM x Distance"]
    FORMULA --> DISCOUNTCHECK{Passenger Type}
    DISCOUNTCHECK -->|Student, Senior, PWD| APPLY20[Apply 20% Discount]
    DISCOUNTCHECK -->|Regular| NODISCOUNT[Full Fare]
    APPLY20 --> FINALFARE[Final Fare]
    NODISCOUNT --> FINALFARE
```

## How route data gets built

No manual route drawing screen for now. The operator phone's GPS quietly logs where the jeepney actually stops while driving its normal shift. Those points sync up and admin approves real stops into the master route table. This keeps the map honest to what jeepneys actually do, and filters GPS jitter before it pollutes the real data.

## Fare calculation, used by Card System and Trip Planner both

Distance is measured between tap in stop and tap out stop, using coordinates from the route data, not straight line air distance. Discount type is set on the passenger's account profile ahead of time, ID verified once at registration or by admin.

Next: [Trip Planner](04-trip-planner.md), [Card System](02-card-system.md).
