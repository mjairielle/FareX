# Admin Panel

Back to [Master Overview](00-master-overview.md).

```mermaid
flowchart TD
    LOGIN[Admin Login] --> ADASH[Admin Dashboard]
    ADASH --> MUSERS[Manage Users]
    ADASH --> MROUTES[Manage Routes]
    ADASH --> MFARES[Manage Fares]
    ADASH --> MVEHICLES[Manage Vehicles]
    ADASH --> MPROMO[Manage Promotions]

    MUSERS --> ULIST[List Passengers and Operators]
    ULIST --> USUSPEND[Suspend or Reinstate Account]
    ULIST --> UDISCOUNT[Verify or Update Discount Type: student, senior, PWD]

    MROUTES --> RREVIEW["Review GPS Submitted Stops, from Operator App"]
    RREVIEW --> RAPPROVE{Approve Stop?}
    RAPPROVE -->|Yes| RCOMMIT[Commit to Master Routes CSV]
    RAPPROVE -->|No| RDISCARD[Discard]
    MROUTES --> RCREATE[Manually Add or Edit a Route]

    MFARES --> FEDIT["Edit Base Fare and Rate per KM"]
    FEDIT --> FPUBLISH[Publish New Fare Table, Effective Date]

    MVEHICLES --> VLIST[List Jeepneys]
    VLIST --> VASSIGN[Assign Vehicle to Operator Account]
    VLIST --> VRETIRE[Retire Vehicle]

    MPROMO --> PCREATE["Create Promotion: reward point multiplier, date range, target route optional"]
    PCREATE --> PACTIVATE[Activate, Feeds Rewards System]
```

## Notes

- Fare and route changes publish with an effective date, so operators already mid shift keep the old fare table until the next sync picks up the new one, no fare change mid trip.
- Manage Users is also where the 20% discount flag gets verified, ties into [Transport System](03-transport-system.md) fare calc.
- Manage Promotions feeds straight into [Rewards System](05-rewards-system.md) point multipliers.

Next: [Server Backend](10-server-backend.md).
