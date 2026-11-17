package com.farex.ui.operator;

import java.util.function.IntConsumer;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
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
import javafx.util.Duration;

import farex.shared.user.Operator;

import com.farex.ui.common.Avatar;
import com.farex.ui.common.CommonUi;
import com.farex.ui.common.Icons;
import com.farex.ui.common.Navigator;
import com.farex.ui.common.Theme;
import com.farex.ui.data.DemoAccount;
import com.farex.ui.data.MockData;
import com.farex.ui.trip.TripUi;

// Operator's Main Dashboard (tab 0 of OperatorShell): who is driving, the jeep, this shift's
// revenue/trips/passengers, sync status, and the floating START SHIFT / END SHIFT button
public class OperatorDashboardView extends StackPane {

    private static final Color CHIP_GRAY = Color.web("#2A2A2A");

    private final Label shiftChip = new Label();

    public OperatorDashboardView(IntConsumer showTab) {
        boolean onShift = OperatorState.isOnShift();

        VBox content = new VBox(12,
                titleRow(onShift),
                profileCard(),
                revenueCard(),
                countCards(),
                syncCard(),
                exitLink(onShift));
        content.setPadding(new Insets(4, 18, 96, 18));   // bottom space so the big button hides nothing

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        Button shiftButton = onShift
                ? OperatorUi.endShiftButton(EndShiftDialog::show)
                : OperatorUi.startShiftButton(() -> {
                    OperatorState.startShift();
                    showTab.accept(1);
                });
        StackPane.setAlignment(shiftButton, Pos.BOTTOM_CENTER);
        StackPane.setMargin(shiftButton, new Insets(0, 18, 10, 18));

        getChildren().addAll(scroll, shiftButton);
    }

    // ----- "Ready to drive?" + shift chip -----

    private Node titleRow(boolean onShift) {
        Label title = TripUi.bold(onShift ? "Shift in progress" : "Ready to drive?", 20);
        title.setWrapText(false);
        Region fill = new Region();
        HBox.setHgrow(fill, Priority.ALWAYS);

        if (onShift) {
            updateShiftChip();
            // The time on the chip ticks every 30 s while this screen is open
            Timeline clock = new Timeline(new KeyFrame(Duration.seconds(30), e -> updateShiftChip()));
            clock.setCycleCount(Animation.INDEFINITE);
            clock.play();
            sceneProperty().addListener((obs, was, now) -> {
                if (now == null) {
                    clock.stop();
                }
            });
        } else {
            copyChip(OperatorUi.chip("Shift inactive", Color.web("#BDBDBD"), CHIP_GRAY));
        }

        HBox row = new HBox(8, title, fill, shiftChip);
        row.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(row, new Insets(6, 0, 2, 0));
        return row;
    }

    // "● On shift · 1h 05m"
    private void updateShiftChip() {
        copyChip(OperatorUi.chip("On shift · " + OperatorState.shiftDuration(), Theme.LIME, CHIP_GRAY));
    }

    private void copyChip(Label from) {
        shiftChip.setText(from.getText());
        shiftChip.setGraphic(from.getGraphic());
        shiftChip.setGraphicTextGap(from.getGraphicTextGap());
        shiftChip.setTextFill(from.getTextFill());
        shiftChip.setFont(from.getFont());
        shiftChip.setPadding(from.getPadding());
        shiftChip.setBackground(from.getBackground());
    }

    // ----- Photo, name, contact, Edit, plate, route -----

    private Node profileCard() {
        DemoAccount account = MockData.getCurrentAccount();
        Operator operator = OperatorState.getOperator();

        VBox text = new VBox(2, TripUi.bold(operator.getName(), 16));
        if (account != null) {
            if (!account.getEmail().isEmpty()) {
                text.getChildren().add(CommonUi.small(account.getEmail(), Theme.WHITE));
            }
            if (!account.getPhone().isEmpty()) {
                text.getChildren().add(CommonUi.small(account.getPhone(), Theme.WHITE));
            }
        }
        HBox person = new HBox(16, new Avatar(account, 66), text);
        person.setAlignment(Pos.CENTER_LEFT);

        Label edit = CommonUi.smallBold("Edit", Theme.LIME);
        edit.setCursor(Cursor.HAND);
        edit.setOnMouseClicked(e -> Navigator.go("accountInfo"));
        HBox editRow = new HBox(edit);
        editRow.setAlignment(Pos.CENTER_RIGHT);

        Label stops = CommonUi.smallBold(OperatorState.stopCount() + " stops", Theme.LIME);
        VBox route = new VBox(2, CommonUi.smallBold(OperatorState.ROUTE_NAME, Theme.WHITE), stops);
        route.setAlignment(Pos.CENTER_RIGHT);

        VBox card = OperatorUi.card(
                person,
                editRow,
                OperatorUi.divider(),
                infoRow("Vehicle Plate", OperatorUi.plateChip(operator.getVehiclePlate())),
                infoRow("Route", route));
        card.setSpacing(10);
        return card;
    }

    private HBox infoRow(String label, Node value) {
        Label left = CommonUi.small(label, Theme.WHITE);
        left.setFont(javafx.scene.text.Font.font("System", 12));
        Region fill = new Region();
        HBox.setHgrow(fill, Priority.ALWAYS);
        HBox row = new HBox(8, left, fill, value);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    // ----- SHIFT REVENUE ₱0.00 -----

    private Node revenueCard() {
        return OperatorUi.card(
                OperatorUi.cardTop("SHIFT REVENUE", Theme.WHITE, Icons.view("icon_op_cash", 20)),
                OperatorUi.money(OperatorState.collectedCents(), 36));
    }

    // ----- COMPLETED (trips) and TOTAL BOARDED (passengers) -----

    private Node countCards() {
        VBox trips = OperatorUi.card(
                OperatorUi.cardTop("COMPLETED", Theme.WHITE, Icons.view("icon_op_trips", 16)),
                OperatorUi.count(OperatorState.getTrips(), "trips"),
                CommonUi.small("Scheduled today", Theme.GRAY));
        VBox boarded = OperatorUi.card(
                OperatorUi.cardTop("TOTAL BOARDED", Theme.WHITE, Icons.view("icon_op_people", 18)),
                OperatorUi.count(OperatorState.getPassengers(), "passengers"),
                CommonUi.small("Fare scans counted", Theme.GRAY));
        for (VBox card : new VBox[] {trips, boarded}) {
            card.setSpacing(4);
            HBox.setHgrow(card, Priority.ALWAYS);
            card.setMaxWidth(Double.MAX_VALUE);
            card.setPrefWidth(0);   // both cards share the width equally
        }
        return new HBox(10, trips, boarded);
    }

    // ----- SYNC STATUS: "12 records not yet synced" + Sync now -----

    private Node syncCard() {
        int pending = OperatorState.pendingRecords();
        Label chip = (pending > 0)
                ? OperatorUi.chip("Pending", OperatorUi.AMBER, Color.web("#3A2A0A"))
                : OperatorUi.chip("Synced", Theme.LIME, Color.web("#2C3308"));

        String headline = (pending == 0) ? "All records synced"
                : pending + (pending == 1 ? " record" : " records") + " not yet synced";

        Button sync = new Button("⟳  Sync now");
        Theme.paintButton(sync, "#141414", "#1C1C1C", "#0A0A0A", 8, "#CAFE06");
        sync.setBorder(new Border(new BorderStroke(Color.web("#3A3A3A"), BorderStrokeStyle.SOLID,
                new CornerRadii(8), new BorderWidths(1))));
        sync.setFont(Theme.bold(12));
        sync.setMaxWidth(Double.MAX_VALUE);
        sync.setPadding(new Insets(10));
        sync.setOnAction(e -> Navigator.go("operatorSync"));

        VBox card = OperatorUi.card(
                OperatorUi.cardTop("SYNC STATUS", Theme.LIME, chip),
                TripUi.bold(headline, 16),
                CommonUi.small("Last synced: " + OperatorState.getLastSynced(), Theme.WHITE),
                sync);
        VBox.setMargin(sync, new Insets(4, 0, 0, 0));
        return card;
    }

    // ----- "Log out / Exit Operator Mode" -----

    // During a shift it asks to end the shift first (the summary has "Back to Passenger mode")
    private Node exitLink(boolean onShift) {
        Label link = CommonUi.link("Log out / Exit Operator Mode",
                () -> {
                    if (onShift) {
                        EndShiftDialog.show();
                    } else {
                        Navigator.go("profile");
                    }
                });
        link.setFont(Theme.bold(12));
        link.setTextFill(Color.web("#BDBDBD"));
        return link;
    }
}
