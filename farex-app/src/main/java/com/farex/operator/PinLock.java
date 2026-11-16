package com.farex.operator;

/**
 * Manages operator terminal lock state and PIN verification.
 * Single Responsibility: Terminal lock security.
 */
public class PinLock {
    private boolean locked = false;
    private String pin = "1234";

    public PinLock() {}

    public PinLock(String pin) {
        this.pin = pin;
    }

    public boolean isLocked() {
        return locked;
    }

    public void lock() {
        this.locked = true;
    }

    public boolean unlock(String enteredPin) {
        if (pin.equals(enteredPin)) {
            this.locked = false;
            return true;
        }
        return false;
    }

    public void setPin(String newPin) {
        if (newPin != null && !newPin.isBlank()) {
            this.pin = newPin;
        }
    }
}
