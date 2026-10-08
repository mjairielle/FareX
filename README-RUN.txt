1. Unzip at the fareX root (same level as farex-shared). Say yes to overwrite.
2. DELETE: farex-app/src/main/java/com/farex/FakeUsers.java
3. From fareX root: mvn -q install -DskipTests
4. Self check, no GUI:
   java -ea -cp farex-shared/target/classes farex.shared.data.StoreDemo
   Last line must be: ALL CHECKS PASSED
5. Run the app the same way you ran the login screen. Run from the farex-app folder.
   CSV files appear in farex-app/data/ on first run. Delete that folder to reset.

Demo accounts (password):
  ana@mail.com 1234        passenger, STUDENT, card 00 6767 6767, P500 loaded
  ben@mail.com 1234        operator
  admin@mail.com admin123  admin
