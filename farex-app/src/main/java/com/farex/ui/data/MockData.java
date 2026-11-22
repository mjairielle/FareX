package com.farex.ui.data;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.farex.Api;

import farex.shared.txn.Txn;
import farex.shared.user.Operator;
import farex.shared.user.Passenger;
import farex.shared.user.User;

/**
 * The logged-in account the screens read. login() fills it from the server (Api).
 *
 * Fake accounts below are only a fallback: with DEV=1 and no server running,
 * aron01@gmail.com / farex123 still logs in with demo cards.
 * shortcut: mock fallback is for demos without a laptop server, delete it once the server always runs.
 */
public final class MockData {

    public static final String DEMO_CONTACT = "aron01@gmail.com";
    public static final String DEMO_PASSWORD = "farex123";

    private static final Map<String, DemoAccount> accounts = new LinkedHashMap<>();
    private static DemoAccount currentAccount;
    private static User liveUser;   // null = the current account is a mock one; else the role the server token acts as
    private static List<String> roles = List.of();     // "PASSENGER", "OPERATOR": every role of the account
    private static List<String> pending = List.of();   // roles an admin has not approved yet

    private static final boolean DEV = "1".equals(System.getenv("DEV")) || "1".equals(System.getProperty("farex.dev"));
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.US);
    private static String pendingPassword = "";
    private static String pendingName = "";
    private static String pendingPhone = "";
    private static String pendingOtp = "";      // pass from the sign-up email code

    // The email/number typed on the Register screen, shown later on the OTP screen
    private static String pendingContact = "";

    // References: the demo's waiting load is FX-LD-1024, the next one gets 1025
    private static int nextReference = 1025;

    static {
        DemoAccount demo = new DemoAccount(DEMO_CONTACT, DEMO_PASSWORD);
        demo.setFullName("Aron Ferrancullo");
        demo.setPhone("09123456789");
        DemoCard aron = new DemoCard("0067676767", "Aron Ferrancullo", 676.50, "09/29");
        aron.addPendingLoad(new PendingLoad(200.00, "GCash", "FX-LD-1024"));   // ready to fetch
        demo.addCard(aron);
        demo.addPoints(250);
        demo.addCard(new DemoCard("0011111111", "Kenn Urbano", 150.00, "10/29"));
        demo.addCard(new DemoCard("0031313131", "Andrei Ng", 0.00, "11/29"));
        addSampleMessages(demo, true);    // bottom: "Other Messages"
        addSampleMessages(demo, false);   // top: "Latest"
        addSampleTransactions(demo);
        accounts.put(DEMO_CONTACT.toLowerCase(), demo);
    }

    // The three messages from the Figma. Each one goes on top, so they are added last-first.
    private static void addSampleMessages(DemoAccount account, boolean read) {
        account.addNotification(new DemoNotification("Load Fetched",
                "Your load has been **successfully** fetched.", read));
        account.addNotification(new DemoNotification("Rewards Claimed",
                "You have claimed **₱50.00** rewards.", read));
        account.addNotification(new DemoNotification("Buy Load Transaction",
                "You have paid **₱300.00** using GCash.", read));
    }

    // The Transactions history from the Figma (Sept 23-25, 2026), oldest first so the newest ends on top
    private static void addSampleTransactions(DemoAccount account) {
        String trip = "Location › Location";
        sample(account, "2026-09-23T07:10", DemoTransaction.FARE, "Pay Fare", trip, -13);
        sample(account, "2026-09-23T09:30", DemoTransaction.FARE, "Pay Fare", trip, -21);
        sample(account, "2026-09-23T13:15", DemoTransaction.FARE, "Pay Fare", trip, -11);
        sample(account, "2026-09-23T17:40", DemoTransaction.FARE, "Pay Fare", trip, -15);
        sample(account, "2026-09-24T08:05", DemoTransaction.FARE, "Pay Fare", trip, -11);
        sample(account, "2026-09-24T14:20", DemoTransaction.TRANSFER, "Balance Transfer",
                "Old xPress Card › New xPress Card", 325);
        sample(account, "2026-09-25T08:43", DemoTransaction.FARE, "Pay Fare", "8:43 AM | " + trip, -35);
        sample(account, "2026-09-25T10:00", DemoTransaction.LOAD, "Buy Load", "10:00 AM | BPI", 200);
        sample(account, "2026-09-25T15:00", DemoTransaction.LOAD, "Buy Load", "3:00 PM | GCash", 500);
    }

    private static void sample(DemoAccount account, String when, String kind, String title,
                               String detail, double amount) {
        account.addTransaction(new DemoTransaction(LocalDateTime.parse(when), kind, title, detail, amount));
    }

    private MockData() {
    }

    /** True when the logged-in account comes from the server, so screens must call Api to change money. */
    public static boolean isLive() {
        return liveUser != null;
    }

    /**
     * Server login. Returns null when logged in, else the message for the red label.
     * The server knows the roles; the login screen then opens the chooser or that role's home.
     */
    public static String login(String contact, String password) {
        try {
            Api.Login login = Api.login(contact, password);
            if (!(login.user() instanceof Passenger || login.user() instanceof Operator)) {
                Api.logout();
                return "Admins use the admin web page: " + Api.base + "/admin";
            }
            liveUser = login.user();
            roles = login.roles();
            pending = login.pending();
            currentAccount = new DemoAccount(liveUser.getEmail(), "");
            currentAccount.setFullName(liveUser.getName());
            currentAccount.setPhone(login.phone());
            refresh();
            return null;
        } catch (Api.ApiException e) {
            if (DEV && e.getMessage().startsWith("Cannot reach") && checkLogin(contact, password)) {
                return null;   // demo without a server
            }
            return e.getMessage();
        }
    }

    // ----- Roles: one account can be passenger and operator -----

    public static boolean hasRole(String role) {
        return roles.contains(role);
    }

    public static boolean isPending(String role) {
        return pending.contains(role);
    }

    /** Both roles on the account: login shows the "Log in as" chooser (even if one waits for approval). */
    public static boolean hasBothRoles() {
        return hasRole("PASSENGER") && hasRole("OPERATOR");
    }

    /** "PASSENGER" or "OPERATOR": the role the account acts as right now. */
    public static String currentRole() {
        return liveUser instanceof Operator ? "OPERATOR" : "PASSENGER";
    }

    /** The jeep plate of a live operator, else null (mock mode shows the sample jeep). */
    public static String plate() {
        return liveUser instanceof Operator o ? o.getVehiclePlate() : null;
    }

    /** What the jeep's QR holds: "FAREX:JEEP:" + the live operator's id ("o1", the sample jeep, in mock mode). */
    public static String jeepQr() {
        return "FAREX:JEEP:" + (liveUser instanceof Operator o ? o.getId() : "o1");
    }

    /** Makes the server token act as this role. Returns null when done, else why not. */
    public static String useRole(String role) {
        if (!isLive() || role.equals(currentRole())) {
            return null;
        }
        try {
            liveUser = Api.switchRole(role);
            refresh();
            return null;
        } catch (Api.ApiException e) {
            return e.getMessage();
        }
    }

    /** Adds a role to the logged-in account (operator needs a plate and admin approval). Null when done, else why not. */
    public static String addRole(String role, String plate) {
        if (!isLive()) {
            return null;
        }
        try {
            boolean waits = "pending".equals(Api.addRole(role, plate));
            roles = add(roles, role);
            if (waits) {
                pending = add(pending, role);
            }
            return null;
        } catch (Api.ApiException e) {
            return e.getMessage();
        }
    }

    private static List<String> add(List<String> list, String item) {
        List<String> out = new java.util.ArrayList<>(list);
        out.add(item);
        return out;
    }

    /**
     * Reloads cards, balances, history and points from the server. Loads that were paid but not
     * fetched yet stay on their card. Passenger accounts only; does nothing for mock or operator.
     */
    public static void refresh() {
        if (!(liveUser instanceof Passenger) || currentAccount == null) {
            return;
        }
        apply(currentAccount, fetch());
    }

    // What refresh() downloads: network only, so it can run off the UI thread
    // paid = loads PayMongo confirmed and not fetched yet, {sessionId, uid, cents, method}; null = server too old to say
    private record Snapshot(List<String[]> cards, List<Txn> history, long points, List<String[]> paid) {}

    private static Snapshot fetch() {
        List<String[]> cards = Api.myCards();                       // {uid, cents, "frozen" or ""}
        for (String[] c : cards) {
            if (c[1].isEmpty()) c[1] = String.valueOf(Api.balance(c[0]));   // older server: no balance in /cards
        }
        List<String[]> paid;
        try {
            paid = Api.paidLoads();
        } catch (Api.ApiException olderServer) {
            paid = null;                                            // keep the pending loads already shown
        }
        return new Snapshot(cards, Api.history(), Api.rewards().points(), paid);
    }

    /**
     * refresh() without blocking the screen: downloads on a background thread, then updates the data and runs
     * onDone on the UI thread, only when something shown changed (a redraw for nothing made Home flicker).
     * Offline or logged out meanwhile: nothing changes and onDone is not run.
     */
    public static void refreshInBackground(Runnable onDone) {
        DemoAccount account = currentAccount;
        if (!(liveUser instanceof Passenger) || account == null) {
            return;
        }
        Thread t = new Thread(() -> {
            try {
                Snapshot s = fetch();
                javafx.application.Platform.runLater(() -> {
                    if (currentAccount == account) {                // still the same login
                        String before = shown(account);
                        apply(account, s);
                        if (!shown(account).equals(before)) {
                            onDone.run();
                        }
                    }
                });
            } catch (Api.ApiException offline) {
                // keep showing the last known balance
            }
        }, "farex-refresh");
        t.setDaemon(true);
        t.start();
    }

    // What a refresh can change on screen: cards, balances, frozen, points, number of transactions
    private static String shown(DemoAccount account) {
        StringBuilder b = new StringBuilder();
        for (DemoCard c : account.getCards()) {
            b.append(c.getCardNumber()).append(':').append(c.getBalance()).append(':').append(c.isFrozen())
                    .append(':').append(c.hasPendingLoad() ? c.getPendingLoad().getReference() : "").append(';');
        }
        return b.append(account.getPointBalance()).append(';').append(account.getTransactions().size()).toString();
    }

    private static void apply(DemoAccount currentAccount, Snapshot s) {
        Map<String, DemoCard> before = new HashMap<>();
        for (DemoCard card : currentAccount.getCards()) {
            before.put(card.getCardNumber(), card);
        }
        currentAccount.getCards().clear();
        for (String[] c : s.cards()) {
            String uid = c[0];
            // Reuse the card object: a Buy Load in progress adds its pending load to it, and a new object would lose that
            DemoCard card = before.get(uid);
            if (card == null) {
                card = new DemoCard(uid, currentAccount.getFullName(), 0, "--/--");
            }
            card.setBalance(Long.parseLong(c[1]) / 100.0);
            if (!c[2].isEmpty() && !card.isFrozen()) {   // reported lost, money not moved yet (a server never unfreezes)
                card.freeze("Reported lost");
            }
            if (s.paid() != null) {                     // Fetch Load shows only confirmed payments
                List<PendingLoad> loads = new java.util.ArrayList<>();
                for (String[] p : s.paid()) {
                    if (p[1].equals(uid)) {
                        loads.add(new PendingLoad(Long.parseLong(p[2]) / 100.0, p[3].isEmpty() ? "Others" : p[3], p[0]));
                    }
                }
                card.setPendingLoads(loads);
            }
            currentAccount.addCard(card);
        }
        currentAccount.getTransactions().clear();
        List<Txn> history = s.history();                         // newest first
        for (int i = history.size() - 1; i >= 0; i--) {          // addTransaction puts each on top
            currentAccount.addTransaction(toUiRow(history.get(i)));
        }
        currentAccount.addPoints((int) s.points() - currentAccount.getPointBalance());
    }

    // A server ledger row as a Transactions tab line. Row: uuid,type,acc,card,amount,before,after,time,extra1,extra2
    static DemoTransaction toUiRow(Txn txn) {
        String[] r = txn.toRow();
        LocalDateTime when = LocalDateTime.ofInstant(txn.getTime(), ZoneId.systemDefault());
        String at = when.format(TIME) + " | ";
        double pesos = txn.getAmount() / 100.0;
        if (r[1].equals("CARD_LOAD")) {
            switch (r[8]) {
                case "Rewards": return new DemoTransaction(when, DemoTransaction.REWARD, "Points Redeemed", at + "Fare credit", pesos);
                case "Replacement Transfer": return new DemoTransaction(when, DemoTransaction.TRANSFER, "Balance Transfer", at + "From lost card", pesos);
                default: return new DemoTransaction(when, DemoTransaction.LOAD, "Buy Load", at + r[8], pesos);
            }
        }
        if (r[8].equals("LOST_REPLACEMENT")) {
            return new DemoTransaction(when, DemoTransaction.TRANSFER, "Balance Transfer", at + "To new card", -pesos);
        }
        return new DemoTransaction(when, DemoTransaction.FARE, "Pay Fare",
                at + String.format(Locale.US, "%.1f km", Double.parseDouble(r[9])), -pesos);
    }

    // ----- Sign up: Register screen -> OTP -> Account Type -> server -----

    public static void setPendingPassword(String password) {
        pendingPassword = password;
    }

    public static void setPendingOtp(String pass) {
        pendingOtp = pass;
    }

    // From the sign-up "Your details" page
    public static void setPendingProfile(String name, String phone) {
        pendingName = name;
        pendingPhone = phone;
    }

    /**
     * Creates the account on the server from the Register screen's email and password and the
     * details page's name and phone. Returns null on success, else the error.
     */
    public static String registerOnServer(String role, String plate) {
        String email = pendingContact.trim();
        String name = pendingName.isBlank() ? email.split("@")[0] : pendingName;
        try {
            Api.register(name, email, pendingPassword, role, "REGULAR", plate, pendingPhone, pendingOtp);
            return null;
        } catch (Api.ApiException e) {
            if (DEV && e.getMessage().startsWith("Cannot reach")) {
                registerAccount(email, pendingPassword);   // demo without a server
                accounts.get(email.toLowerCase()).setFullName(name);
                accounts.get(email.toLowerCase()).setPhone(pendingPhone);
                return null;
            }
            return e.getMessage();
        }
    }

    /** Checks a mock login. When it succeeds, that account becomes the logged-in one. */
    public static boolean checkLogin(String contact, String password) {
        DemoAccount account = accounts.get(contact.trim().toLowerCase());
        if (account != null && account.passwordMatches(password)) {
            currentAccount = account;
            liveUser = null;
            roles = List.of("PASSENGER", "OPERATOR");   // mock accounts may try both sides
            pending = List.of();
            return true;
        }
        return false;
    }

    /** Called by the Register screen. An existing contact is left untouched. */
    public static void registerAccount(String contact, String password) {
        accounts.putIfAbsent(contact.trim().toLowerCase(), new DemoAccount(contact.trim(), password));
    }

    public static DemoAccount getCurrentAccount() {
        return currentAccount;
    }

    public static void logout() {
        currentAccount = null;
        liveUser = null;
        roles = List.of();
        pending = List.of();
        Api.logout();
    }

    /**
     * Delete account. Live: the server deletes every role and writes off the card balance; needs the
     * password. Mock: removed until the app restarts. Returns null when deleted, else why not.
     */
    public static String deleteCurrentAccount(String password) {
        if (currentAccount == null) {
            return null;
        }
        if (isLive()) {
            try {
                Api.deleteAccount(password);
            } catch (Api.ApiException e) {
                return e.getMessage();
            }
        }
        accounts.remove(currentAccount.getContact().toLowerCase());
        logout();
        return null;
    }

    // Every card in every account with its owner, e.g. 0067676767 -> Aron's account.
    // The Operator's tap terminal uses it to charge fares to real demo cards.
    public static Map<DemoCard, DemoAccount> allCards() {
        Map<DemoCard, DemoAccount> cards = new LinkedHashMap<>();
        for (DemoAccount account : accounts.values()) {
            for (DemoCard card : account.getCards()) {
                cards.put(card, account);
            }
        }
        return cards;
    }

    public static void setPendingContact(String contact) {
        pendingContact = contact;
    }

    public static String getPendingContact() {
        return pendingContact;
    }

    // "FX-LD-1025", then "FX-LD-1026", ...
    public static String nextReference() {
        return nextReference("LD");
    }

    // One counter for every kind: load "LD", redeem "RD", ... -> "FX-RD-1025"
    public static String nextReference(String kind) {
        return "FX-" + kind + "-" + nextReference++;
    }
}
