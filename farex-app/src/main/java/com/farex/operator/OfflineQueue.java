package com.farex.operator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages local transaction queue for offline resilience and batch server synchronization.
 * Single Responsibility: Offline transaction batch queuing and serialization.
 */
public class OfflineQueue {
    private final List<String[]> queuedRows = new ArrayList<>();

    public synchronized void enqueue(String[] row) {
        if (row != null) {
            queuedRows.add(row);
        }
    }

    public synchronized int size() {
        return queuedRows.size();
    }

    public synchronized boolean isEmpty() {
        return queuedRows.isEmpty();
    }

    public synchronized String toBatchString() {
        StringBuilder b = new StringBuilder();
        for (String[] row : queuedRows) {
            b.append(String.join(",", row)).append('\n');
        }
        return b.toString();
    }

    public synchronized void clear() {
        queuedRows.clear();
    }

    public synchronized List<String[]> getQueuedRows() {
        return Collections.unmodifiableList(new ArrayList<>(queuedRows));
    }
}
