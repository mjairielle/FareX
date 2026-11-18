package com.farex;

import com.farex.ui.FarexApp;

/**
 * Entry point launcher for the updated FareX UI.
 * Avoids JavaFX runtime component checks when running from IDEs (VS Code, IntelliJ, Eclipse).
 */
public class Launcher {
    public static void main(String[] args) {
        FarexApp.main(args);
    }
}
