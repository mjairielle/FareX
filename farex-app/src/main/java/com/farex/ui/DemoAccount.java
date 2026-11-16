package com.farex.ui;

import java.util.ArrayList;
import java.util.List;

public class DemoAccount {

    private final String contact;
    private final String password;
    private final List<DemoCard> cards = new ArrayList<>();
    private final List<DemoNotification> notifications = new ArrayList<>();
    private final double points = 0;

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

    public boolean hasCard() {
        return !cards.isEmpty();
    }

    public DemoCard getPrimaryCard() {
        return cards.isEmpty() ? null : cards.get(0);
    }

    public double getPoints() {
        return points;
    }

    // Newest first: a new message goes to the top of the list
    public List<DemoNotification> getNotifications() {
        return notifications;
    }

    public void addNotification(DemoNotification notification) {
        notifications.add(0, notification);
    }
}
