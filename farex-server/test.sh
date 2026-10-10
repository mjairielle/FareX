#!/bin/bash
# End to end self-check. Start the server first: FAKE_PAY=1 PORT=8099 DATA_DIR=/tmp/fx java -cp out farex.server.Main
B=http://localhost:8099; FAIL=0
ok(){ [ "$1" == "$2" ] && echo "PASS $3" || { echo "FAIL $3 (got '$1' want '$2')"; FAIL=1; }; }
j(){ sed -E "s/.*\"$1\":\"([^\"]*)\".*/\1/"; }
code(){ curl -s -o /dev/null -w %{http_code} "$@"; }
AT=$(curl -s -X POST $B/login -d "email=ana@mail.com&password=1234" | j token)
OT=$(curl -s -X POST $B/login -d "email=ben@mail.com&password=1234" | j token)
MT=$(curl -s -X POST $B/login -d "email=admin@mail.com&password=admin123" | j token)
H(){ echo "Authorization: Bearer $1"; }
ok "$(curl -s -X POST $B/login -d "email=ana@mail.com&password=no" | j error)" "Wrong email or password" "wrong password"
ok "$(code $B/cards)" 401 "no token is 401"
ok "$(curl -s -X POST $B/register -d "name=X&email=x@m.com&password=1234&role=ADMIN" | j error)" "Bad role" "cannot self register admin"
ok "$(curl -s -X POST $B/register -d "name=Z&email=z@m.com&password=1234&role=OPERATOR&plate=" | j error)" "Vehicle plate is required" "operator needs plate"
ok "$(curl -s $B/balance?uid=0067676767 -H "$(H $AT)" | j cents)" 50000 "seed balance P500"
ok "$(curl -s -X POST $B/cards/add -d last8=11112222 -H "$(H $AT)" | j uid)" 0011112222 "add card"
ok "$(curl -s $B/balance?uid=0011112222 -H "$(H $OT)" | j cents)" 0 "operator reads any card"
curl -s -X POST $B/register -d "name=Cy&email=cy@m.com&password=1234&role=PASSENGER" >/dev/null
CT=$(curl -s -X POST $B/login -d "email=cy@m.com&password=1234" | j token)
ok "$(curl -s $B/balance?uid=0067676767 -H "$(H $CT)" | j error)" "Card is not yours" "cannot read another's card"
S=$(curl -s -X POST $B/load/start -d "uid=0011112222&cents=10000" -H "$(H $AT)"); SID=$(echo $S | j sessionId)
ok "$(curl -s -X POST $B/load/start -d "uid=0011112222&cents=100" -H "$(H $AT)" | j error)" "Amount must be P5.00 to P5,000.00" "amount too low"
ok "$(curl -s $B/balance?uid=0011112222 -H "$(H $AT)" | j cents)" 0 "no credit before confirm"
curl -s -X POST $B/load/confirm -d "sessionId=$SID" -H "$(H $AT)" >/dev/null
curl -s -X POST $B/load/confirm -d "sessionId=$SID" -H "$(H $AT)" >/dev/null
ok "$(curl -s $B/balance?uid=0011112222 -H "$(H $AT)" | j cents)" 10000 "double confirm credits once"
ok "$(curl -s -X POST $B/load/confirm -d "sessionId=$SID" -H "$(H $CT)" | j error)" "Unknown payment" "other user cannot confirm"
ok "$(code -X POST $B/load/start -d "uid=0011112222&cents=1000" -H "$(H $OT)")" 403 "operator cannot load"
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
