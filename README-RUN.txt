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
- The app today shows mock accounts, cards and taps. Only routes, stops, trip plans and fares come from the server.
- New operators cannot log in until an admin presses Reinstate.
