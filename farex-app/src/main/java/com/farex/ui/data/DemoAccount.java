package com.farex.ui.data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DemoAccount {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.US);

    private final String contact;
    private String password;
    private final List<DemoCard> cards = new ArrayList<>();
    private final List<DemoNotification> notifications = new ArrayList<>();
    private final List<DemoTransaction> transactions = new ArrayList<>();
    private int points = 0;   // xPress Points, 1 point = ₱1 of transit credit

    // ----- Profile -----
    private String fullName = "FareX User";
    private String email = "";
    private String phone = "";
    private String photoUrl = null;   // null = plain drawn avatar

    public DemoAccount(String contact, String password) {
        this.contact = contact;
        this.password = password;
        // Registered with an email -> email; with a number -> phone
        if (contact.contains("@")) {
            email = contact;
        } else {
            phone = contact;
        }
    }

    public String getContact() {
        return contact;
    }

    public boolean passwordMatches(String attempt) {
        return password.equals(attempt);
    }

    public void setPassword(String newPassword) {
        password = newPassword;
    }

    // ----- Cards -----

    public List<DemoCard> getCards() {
        return cards;
    }

    public void addCard(DemoCard card) {
        cards.add(card);
    }

    // The new card takes the old card's place: same spot in the list, so it stays "My Card"
    public void replaceCard(DemoCard oldCard, DemoCard newCard) {
        int index = cards.indexOf(oldCard);
        if (index >= 0) {
            cards.set(index, newCard);
        } else {
            cards.add(newCard);
        }
    }

    // "0088428842" already in the account?
    public boolean hasCardNumber(String fullNumber) {
        for (DemoCard card : cards) {
            if (card.getCardNumber().equals(fullNumber)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasCard() {
        return !cards.isEmpty();
    }

    public DemoCard getPrimaryCard() {
        return cards.isEmpty() ? null : cards.get(0);
    }

    // ----- Points -----

    // Home shows this as "250.00 xPress Points"
    public double getPoints() {
        return points;
    }

    // Redeem uses whole points: 250
    public int getPointBalance() {
        return points;
    }

    public void addPoints(int amount) {
        points += amount;
    }

    // 250 - 100 -> 150
    public void deductPoints(int amount) {
        points -= amount;
    }

    // ----- Notifications -----

    // Newest first: a new message goes to the top of the list
    public List<DemoNotification> getNotifications() {
        return notifications;
    }

    public void addNotification(DemoNotification notification) {
        notifications.add(0, notification);
    }

    // ----- Transactions -----

    // Newest first, like notifications
    public List<DemoTransaction> getTransactions() {
        return transactions;
    }

    public void addTransaction(DemoTransaction transaction) {
        transactions.add(0, transaction);
    }

    // Something just happened in the app, e.g. Buy Load ₱200 by GCash:
    // addTransaction(LOAD, "Buy Load", "GCash", 200) -> "4:05 PM | GCash", + ₱ 200.00, today
    public void addTransaction(String kind, String title, String detail, double amount) {
        LocalDateTime now = LocalDateTime.now();
        addTransaction(new DemoTransaction(now, kind, title, now.format(TIME) + " | " + detail, amount));
    }

    // ----- Profile -----

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String name) {
        fullName = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String newEmail) {
        email = newEmail;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String newPhone) {
        phone = newPhone;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String url) {
        photoUrl = url;
    }
}
