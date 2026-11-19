# Card System (xPress Card)

Back to [Master Overview](00-master-overview.md).

```mermaid
flowchart TD
    PHOME[Passenger Home] --> XCARD[xPress Card Menu]
    XCARD --> LOAD[Load]
    XCARD --> LOST[Lost or Stolen Card]
    XCARD --> VTXN[View Transactions, card only]
    XCARD --> VREWARD[View Rewards]
    XCARD --> LINK[Link Another Card]

    LOAD --> BAL[Show Current Balance]
    BAL --> PAYMETHOD{Payment Method}
    PAYMETHOD -->|GCash| ENTERACC[Enter Account Number]
    PAYMETHOD -->|Maya| ENTERACC
    PAYMETHOD -->|Bank| ENTERACC
    PAYMETHOD -->|Others| ENTERACC
    ENTERACC --> ENTERAMT[Enter Load Amount]
    ENTERAMT --> REVIEW["Review: amount, method, fee if any, total"]
    REVIEW -->|Cancel| BAL
    REVIEW -->|Continue| GATEWAY[Send to Payment Gateway]
    GATEWAY --> RESULT{Payment Result}

    RESULT -->|Failed| FAILMSG["Show Reason: insufficient funds, provider unavailable, timed out"]
    FAILMSG --> PAYMETHOD

    RESULT -->|Success| NFCWRITE[Prompt: Tap Physical Card on Your Phone]
    NFCWRITE --> WRITECHECK{Card Detected?}
    WRITECHECK -->|No, timeout| RETRY[Retry Tap, Amount Held Pending]
    RETRY --> NFCWRITE
    WRITECHECK -->|Yes| SIGNWRITE[Server Signs New Balance, Phone Writes to Card Chip]
    SIGNWRITE --> NEWBAL["Balance Updated: Previous + Load Amount = New"]
    NEWBAL --> TXNRECORD[Transaction Recorded, Local and Server]
    TXNRECORD --> NOTIFY[Push Notification]
    NOTIFY --> VTXN

    LOST --> REPORT[Report Card Lost]
    REPORT --> FREEZE[Server Freezes Old Card UID]
    FREEZE --> ISSUENEW[Issue Replacement Card]
    ISSUENEW --> TRANSFER[Server Ledger Balance Transferred to New Card]

    LINK --> SCANNEW[Tap New Physical Card on Phone]
    SCANNEW --> BINDCARD[Bind Card UID to Account]
    BINDCARD --> CARDLIST[Card Now Shown in Account Card List]
```

## Why balance is held on the card, not just the server

Jeepneys tap in the field, often with weak or no signal. The card itself must carry the true spendable balance so a tap works offline. The server keeps a synced copy of that balance for reporting, rewards math, and lost card recovery, but it can lag behind the card by a few minutes until the operator phone next syncs.

## App shown balance vs card balance

- Passenger app shows last synced balance from server, with a small "as of" timestamp, this can lag.
- If the passenger taps their own card on their own phone, the app reads the exact live balance straight off the chip, no lag.

## Lost card

Old card UID gets frozen server side so it can no longer be spent, even if physically tapped elsewhere. Balance on the frozen card's last synced ledger entry moves to the new card's starting balance.

Next: [Transactions](06-transactions.md), [Rewards System](05-rewards-system.md).
