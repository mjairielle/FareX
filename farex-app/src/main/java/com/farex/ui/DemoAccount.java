package com.farex.ui;

import java.util.ArrayList;
import java.util.List;

public class DemoAccount {

    private final String contact;
    private final String password;
    private final List<DemoCard> cards = new ArrayList<>();
    private final List<DemoNotification> notifications = new ArrayList<>();
    private int points = 0;   // xPress Points, 1 point = ₱1 of transit credit

    public DemoAccount(String contact, String password) {
        this.contact = contact;
        this.password = password;
    }

    public String getContact() {
        return contact;
    }

    public boolean passwordMatches(String attempt) {
        return password.equals(attempt);
    }

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

    // Newest first: a new message goes to the top of the list
    public List<DemoNotification> getNotifications() {
        return notifications;
    }

    public void addNotification(DemoNotification notification) {
        notifications.add(0, notification);
    }
}
