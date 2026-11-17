package com.farex.ui;

// Remembers whether the app is in Operator Mode.
// Login, Register, OTP, Congrats and Account Information are shared with the passenger side;
// they ask this class where to go next. (Part 24 adds the shift data here.)
public final class OperatorState {

    private static boolean operatorMode = false;

    private OperatorState() {
    }

    public static boolean isOperatorMode() {
        return operatorMode;
    }

    public static void enter() {
        operatorMode = true;
    }

    public static void leave() {
        operatorMode = false;
    }

    // ----- Where shared screens go next -----

    // Back arrow on Account Information: Operator Dashboard or the Profile tab
    public static String homeRoute() {
        return operatorMode ? "operatorDashboard" : "profile";
    }

    // After a correct login
    public static String afterLoginRoute() {
        return operatorMode ? "operatorDashboard" : "dashboard";
    }

    public static String loginRoute() {
        return operatorMode ? "operatorLogin" : "login";
    }

    public static String registerRoute() {
        return operatorMode ? "operatorRegister" : "register";
    }
}
