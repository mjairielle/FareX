# Before launch: test on real phones

NFC and GPS in the app are simulated (`SimCardIO`, `GpsSimulator`). Nothing below can be tested on a laptop.

## Must pass on an Android phone
1. App reaches the server over HTTPS. Plain http is blocked by Android 9 and newer.
2. Tap a real card: the app reads the chip UID (8 or 14 hex characters) and sends it to `/tap/in`.
3. Operator phone with screen off for 10 minutes still reads taps. Battery saver on.
4. GPS stop detection with a real jeepney ride, in rain, under a flyover. Check for false stops.
5. Airplane mode mid tap: the app shows a clear error, nothing is charged, retry works.
6. Server restart during a shift: riders still on board can tap out.
7. Two phones tap the same card within one second: only one boards.

## Known limits, decide before public launch
- The card UID is not secret. A copied or emulated UID can ride. The server blocks one card on two jeepneys at once,
  but real protection needs a secure card (for example DESFire) that checks a key.
- Server balance is the truth. The chip holds a sealed receipt of the last load, `/card/verify` reports CURRENT, STALE or FORGED.
- History screens show the newest 500 rows per person.
- CSV only means one server process. Do not run two servers on the same data folder.
- Memory use is about 150 MB per million ledger rows.
