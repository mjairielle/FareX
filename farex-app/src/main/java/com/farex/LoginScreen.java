package com.farex;

import farex.shared.user.User;
import javafx.geometry.Pos;
import javafx.scene.control.*;

public class LoginScreen extends Screen {
    public LoginScreen() {
        this("");
    }

    public LoginScreen(String notice) {
        setAlignment(Pos.CENTER);
        TextField email = field("Email/Contact Number");
        PasswordField pass = password("Password");

        if (!notice.isEmpty()) {
            error.setText(notice);
            error.setStyle("-fx-text-fill: #c8ff00;");
        }

        getChildren().addAll(lime("FareX", 36), email, pass,
                primary("Login", () -> {
                    User u = Session.store.login(email.getText(), pass.getText())
                            .orElseThrow(() -> new IllegalArgumentException("Wrong email or password"));
                    Session.user = u;
                    Nav.show(Nav.home(u));
                }),
                link("No account yet? Sign up", () -> Nav.show(new SignupScreen())),
                error);
    }
}
