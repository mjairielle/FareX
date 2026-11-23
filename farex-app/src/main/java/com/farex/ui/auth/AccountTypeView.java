package com.farex.ui.auth;

import java.util.ArrayList;
import java.util.List;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.util.Duration;

import com.farex.ui.common.Icons;
import com.farex.ui.common.Navigator;
import com.farex.ui.common.Theme;
import com.farex.ui.data.MockData;
import com.farex.ui.operator.OperatorState;

public class AccountTypeView extends StackPane {

    private static final double SHEET_HEIGHT = 540;
    private static final String[] ROLES = {"Passenger", "Operator"};

    private static final Border SELECTED_BORDER = new Border(new BorderStroke(
            Theme.LIME, BorderStrokeStyle.SOLID, new CornerRadii(12), new BorderWidths(2)));
    // Same width as the selected border (but invisible) so cards don't shift when selected
    private static final Border IDLE_BORDER = new Border(new BorderStroke(
            Color.TRANSPARENT, BorderStrokeStyle.SOLID, new CornerRadii(12), new BorderWidths(2)));

    private final List<RoleCard> cards = new ArrayList<>();
    private String selectedRole = "Passenger";

    public AccountTypeView() {
        this(false);
    }

    // chooser = true: the same page after login, "Log in as" Passenger or Operator for an account that has both
    public AccountTypeView(boolean chooser) {
        if (chooser) {
            selectedRole = "OPERATOR".equals(MockData.currentRole()) ? "Operator" : "Passenger";
        }
        setBackground(Theme.solid(Theme.LIME));

        // "FareX" partly hidden by the panel and back arrow
        Text outlinedName = new Text("FareX");
        outlinedName.setFont(Theme.bold(112));
        // A Text never shrinks: at 112 pt it is 326 px wide, wider than a narrow screen. Size it to the screen instead.
        widthProperty().addListener((o, was, w) -> outlinedName.setFont(Theme.bold(Math.min(112, w.doubleValue() * 0.3))));
        outlinedName.setFill(Color.TRANSPARENT);
        outlinedName.setStroke(Theme.BLACK);
        outlinedName.setStrokeWidth(2);
        StackPane.setAlignment(outlinedName, Pos.TOP_CENTER);
        StackPane.setMargin(outlinedName, new Insets(90, 0, 0, 0));

        javafx.scene.Node back = com.farex.ui.common.SubScreen.backButton(Theme.BLACK, () -> {
            if (chooser) {
                MockData.logout();   // back = not logging in after all
                Navigator.go("login");
            } else {
                Navigator.go("signUpDetails");
            }
        });
        StackPane.setAlignment(back, Pos.TOP_LEFT);
        StackPane.setMargin(back, new Insets(14, 0, 0, 8));

        // Dark panel
        Label title = new Label(chooser ? "Log in as" : "Account Type");
        title.setTextFill(Theme.WHITE);
        title.setFont(Theme.bold(26));
        title.setMaxWidth(Double.MAX_VALUE);
        title.setAlignment(Pos.CENTER);
        VBox.setMargin(title, new Insets(0, 0, 14, 0));

        VBox sheet = new VBox(14);
        sheet.setPadding(new Insets(36, 24, 36, 24));
        sheet.setBackground(Theme.roundedTop(Theme.DARK, 28));
        sheet.setPrefHeight(SHEET_HEIGHT);
        sheet.setMinHeight(SHEET_HEIGHT);
        sheet.setMaxHeight(SHEET_HEIGHT);
        DropShadow shadow = new DropShadow(30, Color.rgb(0, 0, 0, 0.6));
        shadow.setOffsetY(-8);
        sheet.setEffect(shadow);
        StackPane.setAlignment(sheet, Pos.BOTTOM_CENTER);

        sheet.getChildren().add(title);
        for (String role : ROLES) {
            // "Passenger" -> icon_passenger, "Operator" -> icon_operator
            boolean waiting = chooser && MockData.isPending(role.toUpperCase());
            RoleCard card = new RoleCard(role, Icons.view("icon_" + role.toLowerCase(), 40),
                    waiting ? "Waiting for admin approval" : null);
            card.setDisable(waiting);
            cards.add(card);
            sheet.getChildren().add(card);
        }
        select(selectedRole);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button createButton = new Button(chooser ? "Continue" : "Create Account");
        Theme.limeButton(createButton);
        createButton.setMaxWidth(Double.MAX_VALUE);
        createButton.setOnAction(e -> {
            if (chooser) {
                OperatorState.switchTo(selectedRole.toUpperCase());   // token acts as that role, then its home
            } else {
                SignUpState.createAccount(selectedRole);   // "Passenger" or "Operator", then Congrats
            }
        });

        sheet.getChildren().addAll(spacer, createButton);
        getChildren().addAll(outlinedName, back, sheet);

        playEntrance(sheet);
    }

    // Panel Animation
    private void playEntrance(VBox sheet) {
        sheet.setTranslateY(SHEET_HEIGHT);
        TranslateTransition rise = new TranslateTransition(Duration.millis(450), sheet);
        rise.setToY(0);
        rise.setInterpolator(Interpolator.EASE_OUT);
        rise.play();

        for (int i = 0; i < cards.size(); i++) {
            RoleCard card = cards.get(i);
            card.setOpacity(0);
            card.setTranslateY(30);

            FadeTransition fade = new FadeTransition(Duration.millis(300), card);
            fade.setToValue(1);
            TranslateTransition slide = new TranslateTransition(Duration.millis(300), card);
            slide.setToY(0);
            slide.setInterpolator(Interpolator.EASE_OUT);

            ParallelTransition show = new ParallelTransition(fade, slide);
            show.setDelay(Duration.millis(300 + i * 100));   // 300, 400 ms
            show.play();
        }
    }

    private void select(String role) {
        selectedRole = role;
        for (RoleCard card : cards) {
            card.setSelected(card.role.equals(role));
        }
    }

    // One Role Card

    private class RoleCard extends HBox {
        private final String role;
        private final Circle dot = new Circle(6);

        RoleCard(String role, Node icon, String note) {
            this.role = role;
            setAlignment(Pos.CENTER_LEFT);
            setSpacing(18);
            setPadding(new Insets(14, 20, 14, 20));
            setCursor(Cursor.HAND);

            Label name = new Label(role);
            name.setTextFill(Theme.WHITE);
            name.setFont(Theme.bold(17));

            Region gap = new Region();
            HBox.setHgrow(gap, Priority.ALWAYS);

            Node text = name;
            if (note != null) {
                Label small = new Label(note);
                small.setTextFill(Color.web("#BDBDBD"));
                small.setFont(Theme.bold(11));
                text = new VBox(2, name, small);
            }
            getChildren().addAll(icon, text, gap, dot);
            setOnMouseClicked(e -> select(role));
            setSelected(false);
        }

        void setSelected(boolean selected) {
            setBackground(Theme.rounded(Theme.FIELD, 12));
            setBorder(selected ? SELECTED_BORDER : IDLE_BORDER);
            dot.setFill(selected ? Theme.LIME : Color.web("#888888"));
            if (selected) {
                dot.setEffect(new DropShadow(10, Theme.LIME));
            } else {
                dot.setEffect(null);
            }
        }
    }
}