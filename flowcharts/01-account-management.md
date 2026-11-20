# Account Management

Back to [Master Overview](00-master-overview.md).

```mermaid
flowchart TD
    START([Start]) --> WELCOME[Welcome Screen]
    WELCOME --> HAVE{Have an Account?}

    HAVE -->|No| REGFORM["Register: email/contact no, password"]
    REGFORM --> ACCTYPE{Account Type}
    ACCTYPE -->|Passenger| CREATEP[Account Created, Download Passenger Files]
    ACCTYPE -->|Operator| CREATEO[Account Created, Download Operator Files]
    CREATEP --> LOGINFORM
    CREATEO --> LOGINFORM

    HAVE -->|Yes| LOGINFORM["Login: email/contact no, password"]
    LOGINFORM --> VERIFY{Credentials Valid?}
    VERIFY -->|No| LOGINFAIL[Show Error, Retry]
    LOGINFAIL --> LOGINFORM
    VERIFY -->|Yes| ROLECHECK{Role Stored on Account}

    ROLECHECK -->|Passenger| PHOME["Passenger Home, see Passenger App"]
    ROLECHECK -->|Operator| OHOME["Operator Home, see Operator App"]
    ROLECHECK -->|Admin| AHOME["Admin Home, see Admin Panel"]
```

## Notes

- Role is fixed at registration, one account cannot switch role later. New role needs a new account.
- "Download Files" means the seed CSV set, station list, fare table, current app version assets, so the app has something to read offline on first run.
- Server is the source of truth for accounts. Registration and login both round trip to the cloud server.
- Admin accounts are created separately by an existing admin, not through the public register screen.

Next: [Passenger App](07-passenger-app.md) or [Operator App](08-operator-app.md) or [Admin Panel](09-admin-panel.md).
