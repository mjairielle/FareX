package com.farex.ui.data;

// One message on the Notification screen. Fake data until the backend sends real ones.
// Text between ** ** is shown in bold, e.g. "You have paid **₱300.00** using GCash."
public class DemoNotification {

    private final String title;
    private final String message;
    private boolean read;

    public DemoNotification(String title, String message, boolean read) {
        this.title = title;
        this.message = message;
        this.read = read;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRead() {
        return read;
    }

    public void markRead() {
        read = true;
    }
}
