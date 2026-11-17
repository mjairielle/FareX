package com.farex.ui.auth;

/** Remembers what was picked during sign-up (until the real backend is connected). */
public final class SignUpState {

    private static String role = "Passenger";

    private SignUpState() {
    }

    public static String getRole() {
        return role;
    }

    public static void setRole(String newRole) {
        role = newRole;
    }
}
