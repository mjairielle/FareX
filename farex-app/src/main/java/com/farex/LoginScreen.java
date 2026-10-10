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
        TextField server = field("Server address");
        server.setText(Api.base);

        if (!notice.isEmpty()) {
            error.setText(notice);
            error.setStyle("-fx-text-fill: #c8ff00;");
        }

        getChildren().addAll(lime("FareX", 36), email, pass,
                primary("Login", () -> {
                    Api.base = server.getText().trim();
                    User u = Api.login(email.getText(), pass.getText());
                    Session.user = u;
                    Nav.show(Nav.home(u));
                }),
                link("No account yet? Sign up", () -> Nav.show(new SignupScreen())),
                text("Server address"), server,
                error);
    }
}
