# Rewards System

Back to [Master Overview](00-master-overview.md).

```mermaid
flowchart TD
    TRIPEND[Trip Completed, Tap Out Recorded] --> KM[Distance in KM for This Trip]
    KM --> EARNPOINTS["Points Earned = Distance in KM x Points per KM"]
    EARNPOINTS --> PROMOCHECK{Active Promotion?}
    PROMOCHECK -->|Yes| BOOST[Apply Promotion Multiplier]
    PROMOCHECK -->|No| NOPROMO[Standard Points]
    BOOST --> CREDIT[Credit Points to Account]
    NOPROMO --> CREDIT
    CREDIT --> LEDGERPTS[(Server Points Ledger)]

    PHOME[Passenger Home] --> XCARD[xPress Card Menu]
    XCARD --> VREWARD[View Rewards]
    VREWARD --> TOTALPTS[Total Points]
    TOTALPTS --> REDEEM[Redeem Points]
    REDEEM --> PICKPERK[Pick a Perk or Fare Credit]
    PICKPERK --> CONFIRM{Confirm Redeem?}
    CONFIRM -->|No| VREWARD
    CONFIRM -->|Yes| ADDTOCARD[Add Value to xPress Card]
    ADDTOCARD --> SUCCESS["Notify: Redeemed Successfully, 30 days to fetch"]
    ADDTOCARD --> FAILNOTE["Notify: Redemption Failed, points not deducted"]
```

## Notes

- Points accrue automatically on every completed trip, no manual claim step.
- Promotions can multiply the point rate for a limited window, admin controlled, see [Admin Panel](09-admin-panel.md).
- Redeemed value is written to the card the same way a load is, so it goes through the same signed NFC write step described in [Card System](02-card-system.md).
- "30 days to fetch" means redeemed points convert to a card credit that must be picked up, by tapping the card, within 30 days or it expires.

Next: [Card System](02-card-system.md), [Transactions](06-transactions.md).
