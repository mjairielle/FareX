package com.farex.ui;

import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;

/** Account Type screen: dropdown with Passenger / Operator / Both, then Create Account. */
public class AccountTypeView extends AuthenticationScreen {

    public AccountTypeView() {
        Label title = new Label("Account Type");
        title.setTextFill(Theme.WHITE);
        title.setFont(Theme.bold(26));
        title.setMaxWidth(Double.MAX_VALUE);
        title.setAlignment(Pos.CENTER);

        ComboBox<String> roleBox = new ComboBox<>(
                FXCollections.observableArrayList("Passenger", "Operator", "Both"));
        roleBox.setValue("Passenger");
        styleRoleBox(roleBox);

        Button createButton = new Button("Create Account");
        Theme.limeButton(createButton);
        createButton.setMaxWidth(Double.MAX_VALUE);
        createButton.setOnAction(e -> {
            SignUpState.setRole(roleBox.getValue());   // "Passenger", "Operator" or "Both"
            Navigator.go("congrats");
        });

        addToCard(title, roleBox, createButton);
    }

    // Dark box with white text; the list that opens is dark too, lime when hovered
    private void styleRoleBox(ComboBox<String> box) {
        box.setMaxWidth(Double.MAX_VALUE);
        // Box look and white arrow (same inline-style reason as Theme.inputField)
        box.setStyle("-fx-background-color: #1F1F1F; -fx-background-radius: 10; "
                + "-fx-border-color: #333333; -fx-border-radius: 10; "
                + "-fx-mark-color: white; -fx-padding: 6 4 6 4;");

        // The text shown inside the closed box
        box.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setTextFill(Theme.WHITE);
                setFont(Theme.bold(15));
            }
        });

        // The rows in the opened list
        box.setCellFactory(list -> new ListCell<String>() {
            {
                hoverProperty().addListener((obs, wasHover, isHover) -> paint());
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setFont(Theme.bold(14));
                paint();
            }

            private void paint() {
                boolean hot = isHover() && !isEmpty();
                setBackground(Theme.solid(hot ? Theme.LIME : Theme.FIELD));
                setTextFill(hot ? Theme.BLACK : Theme.WHITE);
            }
        });
    }
}
