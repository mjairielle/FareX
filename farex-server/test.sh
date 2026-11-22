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
exit $FAIL
