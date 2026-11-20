Run FareX on your laptop
========================
1. Copy .env.example to .env. For a local demo set: DEV=1, FAKE_PAY=1, ADMIN_PASSWORD=(8+ chars), CARD_KEY=(any text).
2. From the FareX root: mvn -q install -DskipTests
3. Terminal 1: run-server.bat      (waits on port 8080, creates data/ on first run, delete data/users.csv etc. to reset)
4. Terminal 2: run-app.bat         (or: cd farex-app, mvn javafx:run)
5. Tests: mvn test     Live exploit check (Git Bash): CP=farex-shared/target/classes:farex-server/target/classes ./security-check.sh

Demo accounts (only when DEV=1), password 1234 for both:
  ana@mail.com   passenger, STUDENT, card 0067676767, P500 loaded
  ben@mail.com   operator
Admin: admin@mail.com with the ADMIN_PASSWORD from .env.

Notes
- Public run: no DEV, no FAKE_PAY. See README.md (Docker) and DEVICE-TEST.md.
- The app logs in, signs up, adds cards, buys load and taps against the server. Start the server first.
  Sign up: email + password (8+ characters with a number and a symbol), OTP, your name and phone, account type.
  Operators also give a jeep plate.
- Buy Load opens PayMongo checkout (or skips it with FAKE_PAY=1). Then use Fetch Load: the balance goes up only once paid.
- One login screen for everyone. The app asks the server which roles the account has:
  passenger only -> Home, operator only -> Operator dashboard, both -> "Log in as" page.
- One email can be passenger and operator (same password). Passenger: Profile > OPERATOR'S MODE > Become an operator.
  The admin approves it (admin page, Users or Vehicles, Approve). Operator: dashboard > Use FareX as a passenger.
- Full story: sign up, add card (8 digits), Buy Load, Fetch Load. Log in as ben@mail.com (operator), Ready Mode, tap the card,
  move 2 stops, tap again. Log back in as the passenger: lower balance and a Pay Fare row.
- Phone: if login says "Cannot reach server", a "Server: ..." link appears. Tap it and type the laptop's Wi-Fi address,
  e.g. http://192.168.1.5:8080
- "The server is out of date": an older server is still running. Close it and start run-server.bat again.
- Delete account (Profile > Privacy) asks for the password, removes both roles and writes off the card balance.
- OTP screens show the demo code (123456). No SMS or email is sent.
- Trip planner = the Routes tab. Drag the map freely. Type in From / To (stops), tap the map ("Start here" / "Go here",
  plus the jeeps passing there) or press the location button. With both ends set, up to 3 routes appear at the bottom;
  tap one to see its steps on the map, then Start trip (phone GPS, or a simulated ride on the laptop).
  110 real Quezon City jeepney routes from OpenStreetMap. The map needs internet (Leaflet and map tiles).
- Refresh the routes from OpenStreetMap (needs internet, replaces the route files; keep a copy first):
  java -cp build-server farex.server.ImportOsmRoutes data osm-cache
- Old mock accounts (aron01@gmail.com / farex123) only work when the app runs with DEV=1 and no server is reachable:
  set DEV=1 in the terminal before run-app.bat.
- New operators cannot log in until an admin presses Reinstate.

Android APK
- Ready APK: install farex-app.apk on the phone (allow "install unknown apps"). It needs Android 5+ on a 64-bit phone.
- Phone and laptop on the same Wi-Fi: on the login screen, when it says it cannot reach the server, tap "Server: ..."
  and type http://<laptop Wi-Fi IP>:8080 (find the IP with ipconfig). Allow Java through the Windows firewall.
- Rebuild the APK: Linux only, see build-android.sh.

