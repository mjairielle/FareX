#!/bin/bash
# End to end self-check against a fresh demo server. From the FareX folder, after mvn package:
#   rm -rf /tmp/fx && DEV=1 FAKE_PAY=1 PORT=8099 DATA_DIR=/tmp/fx \
#     java -cp "farex-server/target/classes:farex-shared/target/classes" farex.server.Main > /tmp/fx.log &
#   LOG=/tmp/fx.log bash farex-server/test.sh
# Sign-up needs an email code. With no mail settings (SMTP_*, Brevo) the server prints it, so LOG must be its output.
B=http://localhost:8099; FAIL=0; LOG=${LOG:-/tmp/fx.log}
ok(){ [ "$1" == "$2" ] && echo "PASS $3" || { echo "FAIL $3 (got '$1' want '$2')"; FAIL=1; }; }
j(){ sed -E "s/.*\"$1\":\"([^\"]*)\".*/\1/"; }
code(){ curl -s -o /dev/null -w %{http_code} "$@"; }
# email -> the one-time pass /register needs (asks for a code, reads it from the server log, verifies it)
signup(){
  curl -s -X POST $B/otp/send -d "purpose=SIGNUP&email=$1" >/dev/null
  local c=""
  for i in 1 2 3 4 5 6 7 8 9 10; do c=$(grep "OTP for $1:" "$LOG" 2>/dev/null | tail -1 | sed -E 's/.*: ([0-9]{6}).*/\1/'); [ -n "$c" ] && break; sleep 0.2; done
  [ -z "$c" ] && { echo "FAIL no code for $1 in $LOG (is LOG the server's output, with no mail settings?)" >&2; FAIL=1; }
  curl -s -X POST $B/otp/verify -d "purpose=SIGNUP&email=$1&code=$c" | j otp
}
AT=$(curl -s -X POST $B/login -d "email=ana@mail.com&password=1234" | j token)
OT=$(curl -s -X POST $B/login -d "email=ben@mail.com&password=1234" | j token)
MT=$(curl -s -X POST $B/login -d "email=admin@mail.com&password=admin123" | j token)
H(){ echo "Authorization: Bearer $1"; }
C=11112222                                                   # a second card for Ana (chip UID, 8 hex)
ok "$(curl -s -X POST $B/login -d "email=ana@mail.com&password=no" | j error)" "Wrong email or password" "wrong password"
ok "$(code $B/cards)" 401 "no token is 401"
ok "$(curl -s -X POST $B/register -d "name=X&email=x@m.com&password=password1&role=ADMIN" | j error)" "Bad role" "cannot self register admin"
ok "$(curl -s -X POST $B/register -d "name=Y&email=y@m.com&password=password1&role=PASSENGER" | j error)" "Email not verified. Request a new code." "sign-up needs the email code"
ZP=$(signup z@m.com)
ok "$(curl -s -X POST $B/register -d "name=Z&email=z@m.com&password=password1&role=OPERATOR&plate=&otp=$ZP" | j error)" "Vehicle plate is required" "operator needs plate"
ok "$(curl -s $B/balance?uid=0067676767 -H "$(H $AT)" | j cents)" 50000 "seed balance P500"
ok "$(curl -s -X POST $B/cards/add -d uid=$C -H "$(H $AT)" | j uid)" $C "add card"
ok "$(curl -s $B/balance?uid=$C -H "$(H $OT)" | j cents)" 0 "operator reads any card"
CP=$(signup cy@m.com)
ok "$(curl -s -X POST $B/register -d "name=Cy&email=cy@m.com&password=password1&role=PASSENGER&otp=$CP" | j status)" active "passenger signs up with the code"
CT=$(curl -s -X POST $B/login -d "email=cy@m.com&password=password1" | j token)
ok "$(curl -s $B/balance?uid=0067676767 -H "$(H $CT)" | j error)" "Card is not yours" "cannot read another's card"
S=$(curl -s -X POST $B/load/start -d "uid=$C&cents=10000" -H "$(H $AT)"); SID=$(echo $S | j sessionId)
ok "$(curl -s -X POST $B/load/start -d "uid=$C&cents=100" -H "$(H $AT)" | j error)" "Amount must be P5.00 to P5,000.00" "amount too low"
ok "$(curl -s $B/balance?uid=$C -H "$(H $AT)" | j cents)" 0 "no credit before confirm"
curl -s -X POST $B/load/confirm -d "sessionId=$SID" -H "$(H $AT)" >/dev/null
curl -s -X POST $B/load/confirm -d "sessionId=$SID" -H "$(H $AT)" >/dev/null
ok "$(curl -s $B/balance?uid=$C -H "$(H $AT)" | j cents)" 10000 "double confirm credits once"
ok "$(curl -s $B/cards -H "$(H $AT)" | grep -c "^$C,10000$")" 1 "/cards line carries the balance"
ok "$(curl -s -X POST $B/load/confirm -d "sessionId=$SID" -H "$(H $CT)" | j error)" "Unknown payment" "other user cannot confirm"
ok "$(code -X POST $B/load/start -d "uid=$C&cents=1000" -H "$(H $OT)")" 403 "operator cannot load"
curl -s -X POST $B/tap/in -d "uid=0067676767&stop=0" -H "$(H $OT)" >/dev/null
ok "$(curl -s -X POST $B/tap/in -d "uid=0067676767&stop=0" -H "$(H $OT)" | j error)" "Card is already tapped in" "double tap in rejected"
R=$(curl -s -X POST $B/tap/out -d "uid=0067676767&stop=5" -H "$(H $OT)")
echo "  tap out: $R"
ok "$(echo $R | j onBoard)" 0 "off the bus after tap out"
NB=$(echo $R | j balance); [ "$NB" -lt 50000 ] && echo "PASS fare charged, balance $NB" || { echo "FAIL not charged"; FAIL=1; }
ok "$(curl -s -X POST $B/tap/out -d "uid=0067676767&stop=5" -H "$(H $OT)" | j error)" "Card is not tapped in" "tap out without tap in"
ok "$(curl -s $B/history -H "$(H $AT)" | grep -c FARE_PAYMENT)" 1 "history has the fare"
ok "$(curl -s $B/stops -H "$(H $AT)" | wc -l)" 6 "6 stops"
ok "$(code $B/admin/summary -H "$(H $AT)")" 403 "passenger blocked from admin"
ok "$(curl -s $B/admin/summary -H "$(H $MT)" | grep -c Users)" 1 "admin summary"
ok "$(code -X POST $B/admin/api/card/unlink -d uid=$C -H "$(H $AT)")" 403 "only admin can unlink a card"
ok "$(curl -s -X POST $B/admin/api/card/unlink -d uid=$C -H "$(H $MT)" | j ok)" 1 "admin unlinks Ana's card"
ok "$(curl -s -X POST $B/cards/add -d uid=$C -H "$(H $CT)" | j uid)" $C "another rider adds the unlinked card"
ok "$(curl -s $B/balance?uid=$C -H "$(H $CT)" | j cents)" 10000 "the money stays on the card"
ok "$(curl -s $B/balance?uid=$C -H "$(H $AT)" | j error)" "Card is not yours" "old owner no longer sees it"
WP=$(signup w@m.com)
ok "$(curl -s -X POST $B/register -d "name=W&email=w@m.com&password=password1&role=OPERATOR&plate=abc 123&otp=$WP" | j error)" \
   "Plate abc 123 is already registered to another operator" "no second operator on Ben's plate ABC-123"
CY=$(curl -s -X POST $B/login -d "email=cy@m.com&password=password1" | j id)
ok "$(code -X POST $B/admin/api/user/delete -d id=$CY -H "$(H $AT)")" 403 "only admin can delete an account"
ok "$(curl -s -X POST $B/admin/api/user/delete -d id=$CY -H "$(H $MT)" | j ok)" 1 "admin deletes Cy's account"
ok "$(code $B/cards -H "$(H $CT)")" 401 "Cy's phone is logged out"
ok "$(curl -s -X POST $B/login -d "email=cy@m.com&password=password1" | j error)" "Wrong email or password" "Cy can no longer log in"
ok "$(curl -s -X POST $B/admin/api/user/delete -d id=a1 -H "$(H $MT)" | j error)" "Cannot delete an admin" "admin account cannot be deleted"
# ----- admin: add, edit, delete everything -----
fewest(){ curl -s "$B/plan?fromLat=14.5876&fromLng=120.9813&toLat=14.7349&toLng=121.0566" | grep "Fewest" | cut -d'|' -f3; }
ok "$(code -X POST $B/admin/api/user/add -d "name=Dan&email=dan@m.com&password=password1&role=OPERATOR&plate=DAN-1" -H "$(H $AT)")" 403 "only admin adds accounts"
DAN=$(curl -s -X POST $B/admin/api/user/add -d "name=Dan&email=dan@m.com&password=password1&role=OPERATOR&plate=DAN-1" -H "$(H $MT)" | j id)
ok "$(curl -s -X POST $B/login -d "email=dan@m.com&password=password1" | j role)" OPERATOR "admin-added operator logs in at once (no approval wait)"
ok "$(curl -s -X POST $B/admin/api/user/edit -d "id=$DAN&name=Daniel&plate=abc 123" -H "$(H $MT)" | j error)" "Plate abc 123 is already registered to another operator" "edit keeps one plate per jeep"
ok "$(curl -s -X POST $B/admin/api/user/edit -d "id=$DAN&name=Daniel&email=daniel@m.com&plate=DAN-2&phone=09171234567" -H "$(H $MT)" | j ok)" 1 "admin edits name, email, plate, phone"
ok "$(curl -s $B/admin/api/users -H "$(H $MT)" | grep -c "^$DAN|Daniel|daniel@m.com|OPERATOR||DAN-2|active|09171234567$")" 1 "edits saved"
ok "$(curl -s -X POST $B/admin/api/vehicle/delete -d "id=$DAN" -H "$(H $MT)" | j ok)" 1 "admin deletes a vehicle"
ok "$(curl -s -X POST $B/login -d "email=daniel@m.com&password=password1" | j error)" "Wrong email or password" "its only role gone: account gone"
ok "$(curl -s -X POST $B/admin/api/card/add -d "uid=ABCD1234&email=ana@mail.com" -H "$(H $MT)" | j uid)" ABCD1234 "admin links a card to a passenger"
ok "$(curl -s $B/cards -H "$(H $AT)" | grep -c "^ABCD1234,")" 1 "the passenger sees it"
curl -s -X POST $B/admin/api/card/freeze -d "uid=ABCD1234&frozen=true" -H "$(H $MT)" >/dev/null
ok "$(curl -s $B/admin/api/cards -H "$(H $MT)" | grep "^ABCD1234|" | cut -d'|' -f5)" true "admin freezes a card"
curl -s -X POST $B/admin/api/card/freeze -d "uid=ABCD1234&frozen=false" -H "$(H $MT)" >/dev/null
ok "$(curl -s $B/admin/api/cards -H "$(H $MT)" | grep "^ABCD1234|" | cut -d'|' -f5)" false "and unfreezes it"
ok "$(fewest)" 2 "museum to SM Fairview: 2 transfers with the OSM routes"
ok "$(curl -s -X POST $B/admin/api/route/add -d "id=BUS1&name=Lawton to SM Fairview&oneway=true&stops=Ayala Blvd,14.5904,120.9853;Quezon Ave,14.6400,121.0100;SM City Fairview,14.7349,121.0566" -H "$(H $MT)" | j ok)" 1 "admin adds a route"
ok "$(fewest)" 0 "planner rides it straight away: one jeep, no transfer"
ok "$(curl -s -X POST $B/admin/api/route/add -d "id=BUS1&stops=A,14.1,121.1;B,14.2,121.2" -H "$(H $MT)" | j error)" "Route BUS1 already exists" "adding never overwrites"
ok "$(curl -s -X POST $B/admin/api/route/add -d "id=BUS1&replace=1&name=Lawton to Fairview&stops=Ayala Blvd,14.5904,120.9853;SM City Fairview,14.7349,121.0566" -H "$(H $MT)" | j ok)" 1 "admin edits a route"
ok "$(curl -s "$B/admin/api/route?id=BUS1" -H "$(H $MT)" | tail -1)" "#Lawton to Fairview|false" "edit saved name and direction"
ok "$(curl -s -X POST $B/admin/api/route/delete -d "id=BUS1" -H "$(H $MT)" | j ok)" 1 "admin deletes a route"
ok "$(fewest)" 2 "planner forgets it"
curl -s -X POST $B/admin/api/promo -d "name=Holiday&multiplier=2&start=2026-12-24&end=2026-12-26" -H "$(H $MT)" >/dev/null
PID=$(curl -s $B/admin/api/promos -H "$(H $MT)" | grep "^Holiday|" | cut -d'|' -f5)
curl -s -X POST $B/admin/api/promo -d "id=$PID&name=Holiday x3&multiplier=3&start=2026-12-24&end=2026-12-26" -H "$(H $MT)" >/dev/null
ok "$(curl -s $B/admin/api/promos -H "$(H $MT)" | grep -c "^Holiday x3|3.0|")" 1 "admin edits a promotion"
curl -s -X POST $B/admin/api/promo/delete -d "id=$PID" -H "$(H $MT)" >/dev/null
ok "$(curl -s $B/admin/api/promos -H "$(H $MT)" | grep -c Holiday)" 0 "admin deletes a promotion"
curl -s -X POST $B/admin/api/fares/publish -d "base=1500&rate=225&baseKm=4&effective=2020-01-01" -H "$(H $MT)" >/dev/null
FR=$(curl -s $B/admin/api/fares -H "$(H $MT)" | grep "^2020-01-01|")
ok "$(curl -s $B/admin/api/fares -H "$(H $MT)" | head -1 | cut -d'|' -f2)" 1500 "published fare is in use"
ok "$(curl -s -X POST $B/admin/api/fares/delete -d "effective=2020-01-01&published=$(echo $FR | cut -d'|' -f4)" -H "$(H $MT)" | j ok)" 1 "admin deletes a fare table"
ok "$(curl -s $B/admin/api/fares -H "$(H $MT)" | head -1 | cut -d'|' -f2)" 1400 "back to the built-in fare"
exit $FAIL
