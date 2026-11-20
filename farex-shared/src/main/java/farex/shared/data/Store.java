package farex.shared.data;

import farex.shared.txn.*;
import farex.shared.user.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

/** All CSV reads and writes. Files are append only. Balance is the last ledger row of a card. */
public class Store {
    private static final String H_USERS = "id,name,email,passwordHash,role,passengerType,vehiclePlate,phone";   // phone is optional (older rows have 7 columns)
    private static final String H_CARDS = "uid,ownerId";
    private static final String H_LEDGER = "uuid,type,accountId,cardUid,amount,balanceBefore,balanceAfter,time,extra1,extra2";
    private static final String H_STOPS = "seq,name,lat,lng";

    private static final String H_BOARD = "uid,operatorId,stopIdx,time,event";
    /** A boarding with no tap out older than this no longer blocks the card, one shift. */
    private static final long BOARD_TTL_SECONDS = 12 * 3600;
    private static final int PBKDF2_ITERATIONS = Integer.getInteger("farex.pbkdf2", 210_000);
    private static final java.security.SecureRandom RANDOM = new java.security.SecureRandom();
    private static final String DUMMY_HASH = hashPassword("dummy-to-equalize-login-time");

    /** Newest rows kept in memory per passenger and per operator for the history screens. */
    private static final int HISTORY_CAP = 500;

    private final Path users, cards, ledger, stops, frozen, boarding;

    // In memory copies of the append only files. Each one reads only new rows, see TailFile.
    private final TailFile ledgerTail, cardsTail, frozenTail, boardTail;
    private final Map<String, Long> balances = new HashMap<>();
    private final Map<String, Integer> counts = new HashMap<>();
    private final Set<String> uuids = new HashSet<>();
    private final Map<String, ArrayDeque<String[]>> byAccount = new HashMap<>();
    private final Map<String, ArrayDeque<String[]>> byOperator = new HashMap<>();
    private final Map<String, String> cardOwner = new HashMap<>();
    private final Map<String, List<String>> ownerCards = new HashMap<>();
    private final List<String> cardList = new ArrayList<>();
    private final Set<String> frozenSet = new HashSet<>();
    private final Map<String, String[]> lastBoard = new HashMap<>();

    /** Demo data and the demo admin password. Tests and local runs use this. */
    public Store(Path dir) { this(dir, "admin123", true); }

    /** First run seeds one admin with adminPassword. demo adds the sample passenger, operator and card. */
    public Store(Path dir, String adminPassword, boolean demo) {
        users = dir.resolve("users.csv");
        cards = dir.resolve("cards.csv");
        ledger = dir.resolve("ledger.csv");
        stops = dir.resolve("stops.csv");
        frozen = dir.resolve("frozen.csv");
        boarding = dir.resolve("boarding.csv");
        ledgerTail = new TailFile(ledger);
        cardsTail = new TailFile(cards);
        frozenTail = new TailFile(frozen);
        boardTail = new TailFile(boarding);
        if (!Files.exists(users)) seed(adminPassword, demo);
    }

    // ---------- users ----------

    public User register(String name, String email, String password, String role,
                         String passengerType, String plate) {
        name = Csv.clean(name).trim();
        email = email.trim().toLowerCase();
        if (name.isEmpty() || email.isEmpty()) throw new IllegalArgumentException("Name and email are required");
        if (!email.matches("[^\\s,@]+@[^\\s,@]+")) throw new IllegalArgumentException("Email is not valid");
        if (password.length() < 8) throw new IllegalArgumentException("Password needs 8 or more characters");
        if (role.equals("OPERATOR") && plate.isBlank()) throw new IllegalArgumentException("Vehicle plate is required");
        // One email may hold a passenger row and an operator row, sharing name and password
        final String mail = email;
        List<String[]> same = Csv.read(users, 7).stream().filter(r -> r[2].equals(mail)).toList();
        for (String[] r : same) if (r[4].equals(role) || r[4].equals("ADMIN")) throw new IllegalArgumentException("Email already registered");
        if (!same.isEmpty()) {
            if (!checkPassword(password, same.get(0)[3]))
                throw new IllegalArgumentException("This email already has a FareX account. Use the same password to add the " + role.toLowerCase() + " role.");
            name = same.get(0)[1];
        }
        return appendRole(name, email, hashPassword(password), role, passengerType, plate);
    }

    /** Logged-in user adds the other role to their account, e.g. a passenger becomes an operator too. */
    public User addRole(String userId, String role, String plate) {
        if (!role.equals("PASSENGER") && !role.equals("OPERATOR")) throw new IllegalArgumentException("Bad role");
        if (role.equals("OPERATOR") && plate.isBlank()) throw new IllegalArgumentException("Vehicle plate is required");
        String[] me = Csv.read(users, 7).stream().filter(r -> r[0].equals(userId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No such user"));
        if (me[4].equals("ADMIN")) throw new IllegalArgumentException("Admins cannot add roles");
        for (User u : siblings(userId)) if (u.role().equals(role)) throw new IllegalArgumentException("Account already has the " + role.toLowerCase() + " role");
        return appendRole(me[1], me[2], me[3], role, "REGULAR", plate);
    }

    private User appendRole(String name, String email, String hash, String role, String passengerType, String plate) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        String type = role.equals("PASSENGER") ? passengerType : "";
        Csv.append(users, H_USERS, id, name, email, hash, role, type, Csv.clean(plate).trim());
        return findUser(id).get();
    }

    /** Every role row of the same email, this one included. */
    public List<User> siblings(String userId) {
        String email = findUser(userId).map(User::getEmail).orElse(null);
        return Csv.read(users, 7).stream().filter(r -> r[2].equals(email)).map(Store::userFromRow).toList();
    }

    public Optional<User> login(String loginId, String password) {
        return loginAll(loginId, password).stream().findFirst();
    }

    /** Every role of the account (passenger and/or operator), or empty when the login is wrong. */
    public List<User> loginAll(String loginId, String password) {
        String clean = loginId.trim().toLowerCase();
        for (String[] r : Csv.read(users, 7)) {
            boolean match = r[2].trim().toLowerCase().equals(clean)
                    || r[1].trim().toLowerCase().equals(clean)
                    || r[0].trim().toLowerCase().equals(clean);
            if (match && checkPassword(password, r[3])) {
                if (!r[3].startsWith("pbkdf2$")) setPasswordHash(r[0], hashPassword(password));   // upgrade old SHA-256 hash
                return siblings(r[0]);
            }
            if (match) return List.of();
        }
        checkPassword(password, DUMMY_HASH);          // unknown user costs the same time as a wrong password
        return List.of();
    }

    public Optional<User> findUser(String id) {
        return Csv.read(users, 7).stream().filter(r -> r[0].equals(id)).map(Store::userFromRow).findFirst();
    }

    private String emailOf(String id) {
        return Csv.read(users, 7).stream().filter(r -> r[0].equals(id)).map(r -> r[2]).findFirst().orElse("\u0000");
    }

    private static User userFromRow(String[] r) {
        return switch (r[4]) {
            case "PASSENGER" -> new Passenger(r[0], r[1], r[2], r[5]);
            case "OPERATOR" -> new Operator(r[0], r[1], r[2], r[6]);
            case "ADMIN" -> new Admin(r[0], r[1], r[2]);
            default -> throw new IllegalStateException("Bad role in users.csv: " + r[4]);
        };
    }

    /**
     * Admin fixes a passenger's discount type. The one place a file is rewritten, because users are a table and not a ledger.
     * Type must be REGULAR, STUDENT, SENIOR or PWD.
     */
    public void setPassengerType(String id, String type) {
        if (!List.of("REGULAR", "STUDENT", "SENIOR", "PWD").contains(type)) throw new IllegalArgumentException("Bad passenger type");
        List<String[]> rows = Csv.read(users);
        boolean hit = false;
        for (String[] r : rows) if (r.length >= 7 && r[0].equals(id) && r[4].equals("PASSENGER")) { r[5] = type; hit = true; }
        if (!hit) throw new IllegalArgumentException("No such passenger");
        Csv.rewrite(users, H_USERS, rows);
    }

    public void updateUserName(String id, String newName) {
        newName = Csv.clean(newName).trim();
        if (newName.isBlank()) throw new IllegalArgumentException("Name cannot be empty");
        String email = emailOf(id);
        List<String[]> rows = Csv.read(users);
        for (String[] r : rows) if (r.length >= 7 && r[2].equals(email)) r[1] = newName;
        Csv.rewrite(users, H_USERS, rows);
    }

    /** Phone belongs to the account, so every role row of the email gets it. "" clears it. */
    public void setPhone(String id, String phone) {
        phone = phone.trim();
        if (!phone.isEmpty() && !phone.matches("09\\d{9}")) throw new IllegalArgumentException("Phone must be 11 digits starting with 09");
        String email = emailOf(id);
        List<String[]> rows = Csv.read(users);
        for (int i = 0; i < rows.size(); i++) {
            String[] r = rows.get(i);
            if (r.length >= 7 && r[2].equals(email)) {
                String[] longer = Arrays.copyOf(r, Math.max(8, r.length));
                longer[7] = phone;
                rows.set(i, longer);
            }
        }
        Csv.rewrite(users, H_USERS, rows);
    }

    public String phoneOf(String id) {
        String email = emailOf(id);
        return Csv.read(users, 7).stream().filter(r -> r[2].equals(email) && r.length > 7 && !r[7].isBlank())
                .map(r -> r[7]).findFirst().orElse("");
    }

    /**
     * Closes the account for good: every role row of the email is removed, each card's balance is
     * written off in the ledger (kept for audit) and the card is frozen so nobody can ride on it.
     */
    public void deleteAccount(String id) {
        List<User> roles = siblings(id);
        if (roles.isEmpty()) throw new IllegalArgumentException("No such user");
        if (roles.stream().anyMatch(u -> u.role().equals("ADMIN"))) throw new IllegalArgumentException("Cannot delete an admin");
        List<String> ids = roles.stream().map(User::getId).toList();
        for (String uid : allCards()) {
            String owner = ownerOf(uid);
            if (!ids.contains(owner)) continue;
            long bal = balance(uid);
            if (bal > 0) Csv.append(ledger, H_LEDGER, new FarePayment(UUID.randomUUID().toString(), owner, uid, bal, bal, 0,
                    Instant.now(), "ACCOUNT_DELETED", 0).toRow());
            freezeCard(uid);
        }
        String email = roles.get(0).getEmail();
        List<String[]> rows = Csv.read(users);
        rows.removeIf(r -> r.length >= 7 && r[2].equals(email));
        Csv.rewrite(users, H_USERS, rows);
    }

    public void setPassword(String id, String password) {
        if (password.length() < 8) throw new IllegalArgumentException("Password needs 8 or more characters");
        setPasswordHash(id, hashPassword(password));
    }

    private void setPasswordHash(String id, String hash) {
        String email = emailOf(id);
        List<String[]> rows = Csv.read(users);
        for (String[] r : rows) if (r.length >= 7 && r[2].equals(email)) r[3] = hash;
        Csv.rewrite(users, H_USERS, rows);
    }

    /** True while any account still uses a demo password. The server refuses to run public like that. */
    public boolean hasDefaultLogins() {
        for (String[] r : Csv.read(users, 7)) {              // only the seeded ids, checking every user would be slow
            if (r[0].equals("a1") && checkPassword("admin123", r[3])) return true;
            if ((r[0].equals("p1") || r[0].equals("o1")) && checkPassword("1234", r[3])) return true;
        }
        return false;
    }

    public List<String[]> allVehicles() {
        List<String[]> out = new ArrayList<>();
        for (String[] r : Csv.read(users, 7)) {
            if ("OPERATOR".equals(r[4])) out.add(new String[]{r[6], r[1], r[0]});
        }
        return out;
    }

    // ---------- cards ----------

    /** Card id is the NFC chip's own UID, 4 or 7 bytes as hex. Spaces and colons are ignored. */
    public String addCard(String tagUid, String ownerId) {
        String uid = tagUid.replaceAll("[\\s:]", "").toUpperCase(java.util.Locale.ROOT);
        if (!uid.matches("[0-9A-F]{8}|[0-9A-F]{14}")) throw new IllegalArgumentException("Card UID must be 8 or 14 hex characters");
        if (ownerOf(uid) != null) throw new IllegalArgumentException("Card is already linked to an account");
        Csv.append(cards, H_CARDS, uid, ownerId);
        return uid;
    }

    public synchronized boolean isCardFrozen(String uid) {
        for (String[] r : frozenTail.poll(1, frozenSet::clear)) frozenSet.add(r[0]);
        return frozenSet.contains(uid);
    }

    public void freezeCard(String uid) {
        if (!isCardFrozen(uid)) Csv.append(frozen, "uid", uid);
    }

    /** Freezes old card, issues replacement, transfers remaining balance to new card. */
    public String replaceLostCard(String oldUid, String newTagUid, String ownerId) {
        if (!ownerId.equals(ownerOf(oldUid))) throw new IllegalArgumentException("Card does not belong to this account");
        freezeCard(oldUid);
        String newUid = addCard(newTagUid, ownerId);
        long bal = balance(oldUid);
        if (bal > 0) {
            Txn t1 = new FarePayment(UUID.randomUUID().toString(), ownerId, oldUid, bal, bal, 0,
                    Instant.now(), "LOST_REPLACEMENT", 0);
            Csv.append(ledger, H_LEDGER, t1.toRow());
            Txn t2 = new CardLoad(UUID.randomUUID().toString(), ownerId, newUid, bal, 0, bal,
                    Instant.now(), "Replacement Transfer");
            Csv.append(ledger, H_LEDGER, t2.toRow());
        }
        return newUid;
    }

    private void syncCards() {
        for (String[] r : cardsTail.poll(2, () -> { cardOwner.clear(); ownerCards.clear(); cardList.clear(); })) {
            cardList.add(r[0]);
            if (cardOwner.putIfAbsent(r[0], r[1]) == null) ownerCards.computeIfAbsent(r[1], k -> new ArrayList<>()).add(r[0]);
        }
    }

    public synchronized List<String> cardsOf(String ownerId) {
        syncCards();
        List<String> out = new ArrayList<>();
        for (String uid : ownerCards.getOrDefault(ownerId, List.of())) if (!isCardFrozen(uid)) out.add(uid);
        return out;
    }

    public synchronized List<String> allCards() {
        syncCards();
        return new ArrayList<>(cardList);
    }

    public synchronized String ownerOf(String uid) {
        syncCards();
        return cardOwner.get(uid);
    }

    public static String formatUid(String uid) {
        if (uid.matches("\\d{10}")) return uid.substring(0, 2) + " " + uid.substring(2, 6) + " " + uid.substring(6); // seeded cards
        return uid.replaceAll("(..)(?!$)", "$1 ");
    }

    // ---------- money ----------

    private void syncLedger() {
        for (String[] r : ledgerTail.poll(10, () -> {
            balances.clear(); counts.clear(); uuids.clear(); byAccount.clear(); byOperator.clear(); })) {
            try {
                long after = Long.parseLong(r[6]);
                uuids.add(r[0]);
                balances.put(r[3], after);
                counts.merge(r[3], 1, Integer::sum);
                keep(byAccount, r[2], r);
                if ("FARE_PAYMENT".equals(r[1])) keep(byOperator, r[8], r);
            } catch (NumberFormatException bad) {
                System.err.println("Skipped ledger row with a bad number: " + String.join(",", r));
            }
        }
    }

    private static void keep(Map<String, ArrayDeque<String[]>> m, String key, String[] row) {
        ArrayDeque<String[]> q = m.computeIfAbsent(key, k -> new ArrayDeque<>());
        q.addLast(row);
        if (q.size() > HISTORY_CAP) q.removeFirst();
    }

    public synchronized long balance(String uid) {
        syncLedger();
        return balances.getOrDefault(uid, 0L);
    }

    /** Ledger rows for this card. Goes up on every load or fare, so it is the card's write counter. */
    public synchronized int txnCount(String uid) {
        syncLedger();
        return counts.getOrDefault(uid, 0);
    }

    public Txn load(String uid, String accountId, long amount, String method) {
        return load(uid, accountId, amount, method, UUID.randomUUID().toString());
    }

    /** Same load with a fixed row id. A second call with the same id fails, so a retried payment can never credit twice. */
    public Txn load(String uid, String accountId, long amount, String method, String rowId) {
        if (hasTxn(rowId)) throw new IllegalStateException("Already credited");
        if (isCardFrozen(uid)) throw new IllegalStateException("Card is frozen / reported lost");
        if (amount < 500 || amount > 500000) throw new IllegalArgumentException("Amount must be P5.00 to P5,000.00");
        if (!accountId.equals(ownerOf(uid))) throw new IllegalArgumentException("Card does not belong to this account");
        long before = balance(uid);
        Txn t = new CardLoad(rowId, accountId, uid, amount, before, before + amount,
                Instant.now(), method);
        Csv.append(ledger, H_LEDGER, t.toRow());
        return t;
    }

    public Txn pay(String uid, String operatorId, long amount, double km) { return pay(uid, operatorId, amount, km, 0); }

    /** floor is the lowest balance allowed after paying. A negative floor lets a short rider finish the trip, then the card is blocked until loaded. */
    public Txn pay(String uid, String operatorId, long amount, double km, long floor) {
        if (amount <= 0) throw new IllegalArgumentException("Fare must be more than zero");
        if (isCardFrozen(uid)) throw new IllegalStateException("Card is frozen / reported lost");
        String owner = ownerOf(uid);
        if (owner == null) throw new IllegalStateException("Unknown card");
        long before = balance(uid);
        if (before - amount < floor) {
            throw new IllegalStateException(String.format("Insufficient balance: P%.2f, fare P%.2f",
                    before / 100.0, amount / 100.0));
        }
        Txn t = new FarePayment(UUID.randomUUID().toString(), owner, uid, amount, before, before - amount,
                Instant.now(), operatorId, km);
        Csv.append(ledger, H_LEDGER, t.toRow());
        return t;
    }

    /** Newest first, at most the latest HISTORY_CAP rows. */
    public synchronized List<Txn> history(String accountId) {
        syncLedger();
        return newestFirst(byAccount.get(accountId));
    }

    /** Fare payments taken by an operator, newest first, at most the latest HISTORY_CAP rows. */
    public synchronized List<Txn> operatorHistory(String operatorId) {
        syncLedger();
        return newestFirst(byOperator.get(operatorId));
    }

    private static List<Txn> newestFirst(ArrayDeque<String[]> rows) {
        List<Txn> out = new ArrayList<>();
        if (rows != null) for (Iterator<String[]> it = rows.descendingIterator(); it.hasNext(); ) out.add(Txn.fromRow(it.next()));
        return out;
    }

    public synchronized boolean hasTxn(String uuid) {
        syncLedger();
        return uuids.contains(uuid);
    }

    public void appendTxnRow(String[] row) {
        if (!hasTxn(row[0])) Csv.append(ledger, H_LEDGER, row);
    }

    // ---------- route ----------

    public List<RouteStop> stops() {
        List<RouteStop> out = new ArrayList<>();
        for (String[] r : Csv.read(stops, 4)) out.add(new RouteStop(r[1], Double.parseDouble(r[2]), Double.parseDouble(r[3])));
        return out;
    }

    public String summary() {
        return "Users: " + Csv.read(users).size() + "\nCards: " + Csv.read(cards).size()
                + "\nLedger rows: " + Csv.read(ledger).size() + "\nStops: " + Csv.read(stops).size();
    }

    // ---------- first run ----------

    private void seed(String adminPassword, boolean demo) {
        Csv.append(users, H_USERS, "a1", "Admin", "admin@mail.com", hashPassword(adminPassword), "ADMIN", "", "");
        if (demo) seedDemo();
        seedStops();
    }

    private void seedDemo() {
        Csv.append(users, H_USERS, "p1", "Ana", "ana@mail.com", hashPassword("1234"), "PASSENGER", "STUDENT", "");
        Csv.append(users, H_USERS, "o1", "Ben", "ben@mail.com", hashPassword("1234"), "OPERATOR", "", "ABC-123");
        Csv.append(cards, H_CARDS, "0067676767", "p1");
        load("0067676767", "p1", 50000, "Seed");
    }

    private void seedStops() {
        if (Files.exists(stops)) return;                  // a mounted data folder may already have its own stops
        // Sample coordinates along Commonwealth Ave, QC. Edit stops.csv to change the route.
        String[][] s = {
            {"Philcoa", "14.65400", "121.05377"}, {"Tandang Sora", "14.66223", "121.06619"},
            {"Ever Gotesco", "14.67959", "121.08409"}, {"Batasan", "14.68766", "121.08730"},
            {"Doña Carmen", "14.70517", "121.07843"}, {"Fairview Terminal", "14.73306", "121.05840"}};
        for (int i = 0; i < s.length; i++) Csv.append(stops, H_STOPS, String.valueOf(i), s[i][0], s[i][1], s[i][2]);
    }

    // ---------- who is on a jeepney, shared by every operator ----------

    public void logBoarding(String uid, String operatorId, int stopIdx, String event) {
        Csv.append(boarding, H_BOARD, uid, operatorId, String.valueOf(stopIdx), Instant.now().toString(), event);
    }

    /** card uid to its latest BOARD row {uid, operatorId, stopIdx, time, event}. ALIGHT and stale rows do not count. */
    public synchronized Map<String, String[]> activeBoardings() {
        for (String[] r : boardTail.poll(5, lastBoard::clear)) lastBoard.put(r[0], r);
        Instant cutoff = Instant.now().minusSeconds(BOARD_TTL_SECONDS);
        Map<String, String[]> out = new LinkedHashMap<>(lastBoard);
        out.values().removeIf(r -> !r[4].equals("BOARD") || Instant.parse(r[3]).isBefore(cutoff));
        return out;
    }

    // ---------- passwords ----------

    /** pbkdf2$iterations$salt$hash, no commas so it fits a CSV cell. */
    static String hashPassword(String password) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return "pbkdf2$" + PBKDF2_ITERATIONS + "$" + Base64.getUrlEncoder().withoutPadding().encodeToString(salt)
                + "$" + Base64.getUrlEncoder().withoutPadding().encodeToString(pbkdf2(password, salt, PBKDF2_ITERATIONS));
    }

    static boolean checkPassword(String password, String stored) {
        if (!stored.startsWith("pbkdf2$")) return MessageDigest.isEqual(sha256(password).getBytes(StandardCharsets.UTF_8), stored.getBytes(StandardCharsets.UTF_8));
        String[] p = stored.split("\\$");
        if (p.length != 4) return false;
        byte[] want = Base64.getUrlDecoder().decode(p[3]);
        return MessageDigest.isEqual(want, pbkdf2(password, Base64.getUrlDecoder().decode(p[2]), Integer.parseInt(p[1])));
    }

    private static byte[] pbkdf2(String password, byte[] salt, int iterations) {
        try {
            return javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(new javax.crypto.spec.PBEKeySpec(password.toCharArray(), salt, iterations, 256)).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String sha256(String text) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}