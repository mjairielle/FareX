# Operator App

Back to [Master Overview](00-master-overview.md).

```mermaid
flowchart TD
    LOGIN[Operator Login Success] --> ODASH[Operator Dashboard]
    ODASH --> SHIFTSTART[Start Shift]
    SHIFTSTART --> READYMODE[Phone Always Open, NFC Reader Active, QR Code Displayed]

    READYMODE --> TAPIN{Passenger Taps Card}
    TAPIN -->|Physical Card| READCARD[Read Card UID and Balance]
    TAPIN -->|QR Scan, App User| READAPP[Read App User ID]

    READCARD --> LOGTAPIN[Log Tap In: stop, GPS point, timestamp]
    READAPP --> LOGTAPIN

    LOGTAPIN --> WAITTAPOUT[Wait for Tap Out]
    WAITTAPOUT --> TAPOUT{Passenger Taps Again}
    TAPOUT --> LOGTAPOUT[Log Tap Out: stop, GPS point, timestamp]
    LOGTAPOUT --> DISTANCE[Compute Distance Traveled]
    DISTANCE --> FARE["Fare Calc: base fare + rate per km, minus discount if applicable"]
    FARE --> DEDUCT{Card or App Balance}
    DEDUCT -->|Card| CARDWRITE[Signed Write, Deduct from Card Chip]
    DEDUCT -->|App| SERVERDEDUCT[Deduct from Server Held Balance]

    CARDWRITE --> MAXFARECHECK{No Tap Out Before Route End?}
    SERVERDEDUCT --> MAXFARECHECK
    MAXFARECHECK -->|Yes| CHARGEMAX[Charge Max Route Fare as Fallback]
    MAXFARECHECK -->|No| ROWWRITE[Write Transaction Row]
    CHARGEMAX --> ROWWRITE

    ROWWRITE --> LOCALCSV[(Operator Local CSV)]
    LOCALCSV --> TIMER{Sync Interval Reached or Manual Sync}
    TIMER -->|Yes, and online| UPLOAD[Upload Batch to Server]
    TIMER -->|No, or offline| LOCALCSV
    UPLOAD --> ODASH
```

## Notes

- One operator account per jeepney, login happens once at shift start, not per passenger.
- Tap in without a matching tap out, common if a passenger forgets, gets charged the max fare for that route as a fallback so the operator is never shorted.
- GPS points logged at tap in and tap out double as the raw feed for [Transport System](03-transport-system.md) stop detection.
- Sync retries safely because every row already carries a UUID, see [Transactions](06-transactions.md).

Next: [Transport System](03-transport-system.md), [Server Backend](10-server-backend.md).
