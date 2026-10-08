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
    private static final String H_USERS = "id,name,email,passwordHash,role,passengerType,vehiclePlate";
    private static final String H_CARDS = "uid,ownerId";
    private static final String H_LEDGER = "uuid,type,accountId,cardUid,amount,balanceBefore,balanceAfter,time,extra1,extra2";
    private static final String H_STOPS = "seq,name,lat,lng";

    private final Path users, cards, ledger, stops;

    public Store(Path dir) {
        users = dir.resolve("users.csv");
        cards = dir.resolve("cards.csv");
        ledger = dir.resolve("ledger.csv");
        stops = dir.resolve("stops.csv");
        if (!Files.exists(users)) seed();
    }

    // ---------- users ----------

    public User register(String name, String email, String password, String role,
                         String passengerType, String plate) {
        name = name.trim();
        email = email.trim().toLowerCase();
        if (name.isEmpty() || email.isEmpty()) throw new IllegalArgumentException("Name and email are required");
        if (password.length() < 4) throw new IllegalArgumentException("Password needs 4 or more characters");
        if (findByEmail(email).isPresent()) throw new IllegalArgumentException("Email already registered");
        if (role.equals("OPERATOR") && plate.isBlank()) throw new IllegalArgumentException("Vehicle plate is required");
        String id = UUID.randomUUID().toString().substring(0, 8);
        String type = role.equals("PASSENGER") ? passengerType : "";
        Csv.append(users, H_USERS, id, name, email, sha256(password), role, type, plate.trim());
        return findByEmail(email).get();
    }

    public Optional<User> login(String email, String password) {
        String hash = sha256(password);
        for (String[] r : Csv.read(users)) {
            if (r[2].equals(email.trim().toLowerCase()) && r[3].equals(hash)) return Optional.of(userFromRow(r));
        }
        return Optional.empty();
    }

    public Optional<User> findUser(String id) {
        return Csv.read(users).stream().filter(r -> r[0].equals(id)).map(Store::userFromRow).findFirst();
    }

    private Optional<User> findByEmail(String email) {
        return Csv.read(users).stream().filter(r -> r[2].equals(email)).map(Store::userFromRow).findFirst();
    }

    private static User userFromRow(String[] r) {
        return switch (r[4]) {
            case "PASSENGER" -> new Passenger(r[0], r[1], r[2], r[5]);
            case "OPERATOR" -> new Operator(r[0], r[1], r[2], r[6]);
            default -> new Admin(r[0], r[1], r[2]);
        };
    }

    // ---------- cards ----------

    /** Passenger types the last 8 digits. Card number is "00" plus those digits. */
    public String addCard(String last8, String ownerId) {
        if (!last8.matches("\\d{8}")) throw new IllegalArgumentException("Enter exactly 8 digits");
        String uid = "00" + last8;
        if (ownerOf(uid) != null) throw new IllegalArgumentException("Card is already linked to an account");
        Csv.append(cards, H_CARDS, uid, ownerId);
        return uid;
    }

    public List<String> cardsOf(String ownerId) {
        List<String> out = new ArrayList<>();
        for (String[] r : Csv.read(cards)) if (r[1].equals(ownerId)) out.add(r[0]);
        return out;
    }

    public List<String> allCards() {
        List<String> out = new ArrayList<>();
        for (String[] r : Csv.read(cards)) out.add(r[0]);
        return out;
    }

    public String ownerOf(String uid) {
        for (String[] r : Csv.read(cards)) if (r[0].equals(uid)) return r[1];
        return null;
    }

    public static String formatUid(String uid) {
        return uid.substring(0, 2) + " " + uid.substring(2, 6) + " " + uid.substring(6);
    }

    // ---------- money ----------

    public long balance(String uid) {
        long bal = 0;
        for (String[] r : Csv.read(ledger)) if (r[3].equals(uid)) bal = Long.parseLong(r[6]);
        return bal;
    }

    public Txn load(String uid, String accountId, long amount, String method) {
        if (amount < 500 || amount > 500000) throw new IllegalArgumentException("Amount must be P5.00 to P5,000.00");
        if (!accountId.equals(ownerOf(uid))) throw new IllegalArgumentException("Card does not belong to this account");
        long before = balance(uid);
        Txn t = new CardLoad(UUID.randomUUID().toString(), accountId, uid, amount, before, before + amount,
                Instant.now(), method);
        Csv.append(ledger, H_LEDGER, t.toRow());
        return t;
    }

    public Txn pay(String uid, String operatorId, long amount, double km) {
        String owner = ownerOf(uid);
        if (owner == null) throw new IllegalStateException("Unknown card");
        long before = balance(uid);
        if (before < amount) {
            throw new IllegalStateException(String.format("Insufficient balance: P%.2f, fare P%.2f",
                    before / 100.0, amount / 100.0));
        }
        Txn t = new FarePayment(UUID.randomUUID().toString(), owner, uid, amount, before, before - amount,
                Instant.now(), operatorId, km);
        Csv.append(ledger, H_LEDGER, t.toRow());
        return t;
    }

    /** Newest first. */
    public List<Txn> history(String accountId) {
        List<Txn> out = new ArrayList<>();
        for (String[] r : Csv.read(ledger)) if (r[2].equals(accountId)) out.add(Txn.fromRow(r));
        Collections.reverse(out);
        return out;
    }

    // ---------- route ----------

    public List<RouteStop> stops() {
        List<RouteStop> out = new ArrayList<>();
        for (String[] r : Csv.read(stops)) out.add(new RouteStop(r[1], Double.parseDouble(r[2]), Double.parseDouble(r[3])));
        return out;
    }

    public String summary() {
        return "Users: " + Csv.read(users).size() + "\nCards: " + Csv.read(cards).size()
                + "\nLedger rows: " + Csv.read(ledger).size() + "\nStops: " + Csv.read(stops).size();
    }

    // ---------- first run ----------

    private void seed() {
        Csv.append(users, H_USERS, "a1", "Admin", "admin@mail.com", sha256("admin123"), "ADMIN", "", "");
        Csv.append(users, H_USERS, "p1", "Ana", "ana@mail.com", sha256("1234"), "PASSENGER", "STUDENT", "");
        Csv.append(users, H_USERS, "o1", "Ben", "ben@mail.com", sha256("1234"), "OPERATOR", "", "ABC-123");
        Csv.append(cards, H_CARDS, "0067676767", "p1");
        load("0067676767", "p1", 50000, "Seed");
        // Sample coordinates along Commonwealth Ave, QC. Edit stops.csv to change the route.
        String[][] s = {
            {"Philcoa", "14.6537", "121.0594"}, {"Tandang Sora", "14.6700", "121.0540"},
            {"Ever Gotesco", "14.6810", "121.0480"}, {"Batasan", "14.6850", "121.0870"},
            {"Doña Carmen", "14.6930", "121.0800"}, {"Fairview Terminal", "14.7300", "121.0560"}};
        for (int i = 0; i < s.length; i++) Csv.append(stops, H_STOPS, String.valueOf(i), s[i][0], s[i][1], s[i][2]);
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
