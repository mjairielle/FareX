# Transactions

Back to [Master Overview](00-master-overview.md).

```mermaid
flowchart TD
    EVENT{Event Type} -->|Card Load| ROWLOAD[Write Transaction Row: type Card Load]
    EVENT -->|Fare Payment| ROWFARE[Write Transaction Row: type Fare Payment]
    EVENT -->|Points Redeemed| ROWREDEEM[Write Transaction Row: type Points Redeemed]

    ROWLOAD --> LOCALCSV[(Local CSV, operator or passenger device)]
    ROWFARE --> LOCALCSV
    ROWREDEEM --> LOCALCSV

    LOCALCSV --> UUID[Row Gets a UUID on Creation]
    UUID --> UPLOAD[Upload to Server, on sync interval]
    UPLOAD --> DEDUPE{UUID Already in Server Ledger?}
    DEDUPE -->|Yes| SKIP[Skip, Already Recorded]
    DEDUPE -->|No| APPEND[Append to Master Ledger CSV]
    APPEND --> UPDATEBAL[Update Synced Balance Copy]

    PHOME[Passenger Home] --> TXNTAB[Transactions Tab]
    TXNTAB --> TXNALL[Shows All Types: loads, fares, redemptions]
    TXNALL --> TXNDETAIL["Transaction Detail: type, amount, method, card last 4, previous balance, new balance, date, status, reference no"]

    XCARDVIEW[xPress Card, View Transactions] --> TXNCARDONLY[Shows Card Related Rows Only, subset of full Transactions Tab]
```

## Two transaction views, not a duplicate

- Home "Transactions" tab, everything: card loads, fare taps, reward redemptions.
- xPress Card "View Transactions", filtered to just that card's activity, a narrower view of the same ledger.

## Why append only plus UUID

Operator phones can lose signal mid upload and retry the same batch. Since every row is stamped with a UUID at creation time, retried uploads just get skipped as duplicates on the server side, so nothing double counts and no row ever needs editing after the fact.

Next: [Card System](02-card-system.md), [Server Backend](10-server-backend.md).
