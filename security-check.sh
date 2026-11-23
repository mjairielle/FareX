#!/bin/bash
# Starts the server in production mode and replays the launch review exploits. Exit code 1 if any still works.
# Usage: CP=farex-shared/target/classes:farex-server/target/classes ./security-check.sh
CP=${CP:-farex-shared/target/classes:farex-server/target/classes}
CP=$(for p in ${CP//:/ }; do realpath "$p"; done | paste -sd:)
W=$(mktemp -d); FAIL=0; PORT=18090; U=http://localhost:$PORT
ok(){ echo "PASS  $1"; }; bad(){ echo "FAIL  $1"; FAIL=1; }
start(){ (cd $W && exec env "$@" PORT=$PORT DATA_DIR=$W/data java -cp "$CP" farex.server.Main >$W/srv.log 2>&1) & echo $! > $W/pid; sleep 2; }
stop(){ kill $(cat $W/pid) 2>/dev/null; wait 2>/dev/null; }
post(){ curl -s -m 10 -X POST "$U$1" ${3:+-H "Authorization: Bearer $3"} --data "$2"; }
get(){ curl -s -m 10 "$U$1" ${2:+-H "Authorization: Bearer $2"}; }
tok(){ post /login "email=$1&password=$2" | sed -nE 's/.*"token":"([^"]+)".*/\1/p'; }
# Sign-up needs an email code. Without SMTP settings the server prints it in its log: read it there.
reg(){ local email=$(echo "$1" | sed -nE 's/.*email=([^&]+).*/\1/p')
  post /otp/send "purpose=SIGNUP&email=$email" >/dev/null
  local code=$(grep "OTP for $email:" $W/srv.log | tail -1 | grep -oE '[0-9]{6}' | head -1)
  local pass=$(post /otp/verify "purpose=SIGNUP&email=$email&code=$code" | sed -nE 's/.*"otp":"([^"]+)".*/\1/p')
  post /register "$1&otp=$pass"; }

# 4: fake pay and demo logins cannot run public
start FAKE_PAY=1 ADMIN_PASSWORD=Adm1nPassw0rd CARD_KEY=k1; grep -q "needs DEV=1" $W/srv.log && ok "FAKE_PAY refused without DEV=1" || bad "FAKE_PAY allowed in production"; stop
rm -rf $W/data; start CARD_KEY=k1; grep -q "First run: set ADMIN_PASSWORD" $W/srv.log && ok "first run needs ADMIN_PASSWORD" || bad "first run without admin password"; stop

# 5: clean production start, no demo accounts, admin uses ADMIN_PASSWORD
rm -rf $W/data; start ADMIN_PASSWORD=Adm1nPassw0rd CARD_KEY=k1
[ -n "$(tok admin@mail.com Adm1nPassw0rd)" ] && ok "admin logs in with ADMIN_PASSWORD" || bad "admin login"
[ -z "$(tok admin@mail.com admin123)" ] && ok "admin123 rejected" || bad "admin123 works"
[ -z "$(tok ana@mail.com 1234)" ] && ok "no demo passenger" || bad "demo passenger exists"
TA=$(tok admin@mail.com Adm1nPassw0rd)

# 1: rename with comma cannot become admin
reg "name=Eve&email=eve@x.com&password=pass1234&role=PASSENGER" >/dev/null
TE=$(tok eve@x.com pass1234); post /profile/update "name=a,b" $TE >/dev/null
get /admin/api/users $TE | grep -q '"error"' && ok "renamed passenger still blocked from admin api" || bad "passenger became admin"
[ -n "$(tok eve@x.com pass1234)" ] && ok "renamed passenger can still log in" || bad "rename locked account"

# 2: operator needs approval, sync cannot mint
R=$(reg "name=Mal&email=mal@x.com&password=pass1234&role=OPERATOR&plate=ZZZ-999"); OP=$(echo $R | sed -nE 's/.*"id":"([^"]+)".*/\1/p')
[ -z "$(tok mal@x.com pass1234)" ] && ok "new operator cannot log in before approval" || bad "operator active without approval"
post /admin/api/user/status "id=$OP&action=REINSTATE" $TA >/dev/null
TM=$(tok mal@x.com pass1234); [ -n "$TM" ] && ok "operator logs in after admin approval" || bad "approval does not work"
post /cards/add "uid=AABBCCDD" $TE >/dev/null
post /sync/txns "batch=m1,CARD_LOAD,p1,AABBCCDD,99999999,0,99999999,2026-11-17T00:00:00Z,x," $TM >/dev/null
get "/balance?uid=AABBCCDD" $TM | grep -q '"cents":"0"' && ok "operator sync cannot mint money" || bad "sync minted money"

# 6: poison inputs do not brick the server
reg "name=Bob%0D1,fake,x&email=bob@x.com&password=pass1234&role=PASSENGER" >/dev/null
[ -n "$(tok eve@x.com pass1234)" ] && [ -n "$(tok bob@x.com pass1234)" ] && ok "CR in name does not break login" || bad "CR injection broke login"
get /admin/api/users $TA | grep -q "eve@x.com" && ok "admin user list still works" || bad "admin user list broken"
printf 'effective,base,ratePerKm,published\noops,1,2,3\n' > $W/data/fares.txt
get /fares/current $TE | grep -q '"base"' && ok "bad fares.txt row skipped" || bad "bad fares.txt row breaks requests"
# 7: a card with P0 cannot ride
post /tap/in "uid=AABBCCDD&stop=0" $TM | grep -q "Balance too low" && ok "tap in refused at P0" || bad "P0 card boarded"

# 8: passwords are salted, login locks after 5 wrong tries
grep -qE ',[0-9a-f]{64},' $W/data/users.txt && bad "plain SHA-256 hash in users.txt" || ok "no plain SHA-256 hashes stored"
for i in 1 2 3 4 5; do post /login "email=bob@x.com&password=wrongwrong$i" >/dev/null; done
post /login "email=bob@x.com&password=pass1234" | grep -q "Too many tries" && ok "login locked after 5 wrong tries" || bad "no lockout"
reg "name=Short&email=short@x.com&password=1234567&role=PASSENGER" | grep -q "8 or more" && ok "7 character password rejected" || bad "short password accepted"
# 15: request body cap
head -c 70000 /dev/zero | tr '\0' 'a' | curl -s -m 10 -X POST $U/login --data-binary @- | grep -q "Request too large" && ok "oversized request body refused" || bad "no body size cap"
stop

# 15: one payment can only credit once (DEV server with fake payments)
rm -rf $W/data; start DEV=1 FAKE_PAY=1
TN=$(tok ana@mail.com 1234)
R=$(post /load/start "uid=0067676767&cents=500000" $TN); SID=$(echo $R | sed -nE 's/.*"sessionId":"([^"]+)".*/\1/p')
post /load/confirm "sessionId=$SID" $TN >/dev/null
post /load/confirm "sessionId=$SID" $TN | grep -q '"cents":"550000"' && ok "retried confirm credits once (P500 + P5000)" || bad "confirm credited twice or failed"
# 8: email codes
post /register "name=Nov&email=nov@x.com&password=pass1234&role=PASSENGER" | grep -q "not verified" && ok "sign-up without email code refused" || bad "sign-up without email code"
post /otp/send "purpose=SIGNUP&email=guess@x.com" >/dev/null
for i in 1 2 3 4; do post /otp/verify "purpose=SIGNUP&email=guess@x.com&code=000000" >/dev/null; done
post /otp/verify "purpose=SIGNUP&email=guess@x.com&code=000000" | grep -q "Too many wrong" && ok "5 wrong codes kill the code" || bad "code can be guessed"
post /cards/freeze "uid=0067676767" $TN | grep -q "not verified" && ok "freezing a card needs the email code" || bad "card frozen without email code"
stop; rm -rf $W
exit $FAIL
