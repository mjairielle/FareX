# farex-server step 1
Run from the FareX root so .env is found:
  mvn -q -f farex-server/pom.xml package
  java -jar farex-server/target/farex-server-1.0.jar
Self check (bash or Git Bash): FAKE_PAY=1 PORT=8099 java -jar ... then ./farex-server/test.sh
Data lands in ./data/*.csv
