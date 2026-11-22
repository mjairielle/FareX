package com.farex;

import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

public class SignupScreen extends Screen {
    public SignupScreen() {
        setAlignment(Pos.CENTER);
        TextField name = field("Full name");
        TextField email = field("Email/Contact Number");
        PasswordField pass = password("Password");
        PasswordField confirm = password("Confirm Password");

        ToggleGroup group = new ToggleGroup();
        RadioButton passenger = new RadioButton("Passenger");
        RadioButton operator = new RadioButton("Operator");
        for (RadioButton r : new RadioButton[] { passenger, operator }) {
            r.setToggleGroup(group);
            r.setStyle("-fx-text-fill: white;");
        }
        passenger.setSelected(true);

        ComboBox<String> type = combo();
        type.setItems(FXCollections.observableArrayList("REGULAR", "STUDENT", "SENIOR", "PWD"));
        type.setValue("REGULAR");
        TextField plate = field("Vehicle plate");
        plate.setVisible(false);
        plate.setManaged(false);

        passenger.selectedProperty().addListener((o, was, isPassenger) -> {
            type.setVisible(isPassenger);
            type.setManaged(isPassenger);
            plate.setVisible(!isPassenger);
            plate.setManaged(!isPassenger);
        });

        getChildren().addAll(heading("Create account"), name, email, pass, confirm,
                new HBox(16, passenger, operator), type, plate,
                primary("Create Account", () -> {
                    if (!pass.getText().equals(confirm.getText())) {
                        throw new IllegalArgumentException("Passwords do not match");
                    }
                    Api.register(name.getText(), email.getText(), pass.getText(),
                            passenger.isSelected() ? "PASSENGER" : "OPERATOR", type.getValue(), plate.getText(),
                            "", "");   // old screen, no email code step: the server answers "Email not verified" (new UI: ui/auth)
                    Nav.show(new LoginScreen("Account created. Please log in."));
                }),
                link("Already have an account? Log in", () -> Nav.show(new LoginScreen())),
                error);
    }
}
