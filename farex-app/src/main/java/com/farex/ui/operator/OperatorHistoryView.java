package com.farex.ui.operator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
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

import com.farex.ui.common.CommonUi;
import com.farex.ui.common.Icons;
import com.farex.ui.common.Navigator;
import com.farex.ui.common.Theme;
import com.farex.ui.common.WarningDialog;
import com.farex.ui.trip.TripUi;

// History tab (tab 2 of OperatorShell): Total Collections for this shift, filter chips
// (All Fares / NFC Cards / App QR / Pending sync), then the fare feed: this shift on top,
// the 16 Nov shift below. "Sync pending records" at the bottom while anything waits.
public class OperatorHistoryView extends ScrollPane {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.US);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM", Locale.US);
    private static final Color RED = Color.web("#C62828");

    private enum Filter { ALL, CARD, APP, PENDING }

    private Filter filter = Filter.ALL;
    private final HBox chips = new HBox(6);
    private final VBox feed = new VBox(10);

    public OperatorHistoryView() {
        VBox content = new VBox(12, totalCard(), chipRow(), feed);
        content.setPadding(new Insets(4, 18, 20, 18));
        refresh();

        setContent(content);
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setVbarPolicy(ScrollBarPolicy.NEVER);
        setStyle("-fx-background: transparent; -fx-background-color: transparent;");
    }

    private void refresh() {
        buildChips();
        buildFeed();
    }

    // ----- Total Collections card -----

    private Node totalCard() {
        Label plate = CommonUi.smallBold(OperatorState.plate(), Theme.LIME);
        plate.setGraphic(Icons.view("icon_op_bus", 12));
        plate.setGraphicTextGap(5);
        plate.setPadding(new Insets(3, 9, 3, 7));
        plate.setBackground(Theme.rounded(Color.web("#2C3308"), 50));

        VBox card = OperatorUi.card(
                OperatorUi.cardTop("Total Collections", Theme.LIME, plate),
                OperatorUi.money(OperatorState.collectedCents(), 34),
                TripUi.bold("Shift Fares & Earnings", 16),
                CommonUi.small("Fare transactions collected on vehicle " + OperatorState.plate(), Theme.WHITE),
                CommonUi.small("Route: " + OperatorState.routeName(), Theme.WHITE));
        card.setSpacing(4);
        card.setPadding(new Insets(16, 14, 16, 14));
        return card;
    }

    // ----- Filter chips (slide sideways if they don't fit) -----

    private Node chipRow() {
        ScrollPane row = new ScrollPane(chips);
        row.setPannable(true);
        row.setFitToHeight(true);
        row.setHbarPolicy(ScrollBarPolicy.NEVER);
        row.setVbarPolicy(ScrollBarPolicy.NEVER);
        row.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        return row;
    }

    private void buildChips() {
        int all = matching(Filter.ALL, OperatorState.getRecords()).size()
                + matching(Filter.ALL, OperatorState.getEarlierRecords()).size();
        chips.getChildren().setAll(
                chip("All Fares (" + all + ")", null, Filter.ALL),
                chip("NFC Cards", "icon_op_nfc", Filter.CARD),
                chip("App QR", "icon_op_app_qr", Filter.APP),
                chip("Pending sync", null, Filter.PENDING));
    }

    private Label chip(String text, String icon, Filter which) {
        boolean on = (filter == which);
        Label chip = CommonUi.smallBold(text, on ? Theme.BLACK : Theme.WHITE);
        chip.setFont(Theme.bold(10));
        if (icon != null) {
            Node view = Icons.view(icon, 11);
            view.setOpacity(on ? 0.0 : 1.0);   // white icon disappears on lime; text is enough there
            chip.setGraphic(view);
            chip.setGraphicTextGap(5);
        }
        chip.setPadding(new Insets(7, 12, 7, 12));
        chip.setBackground(Theme.rounded(on ? Theme.LIME : Color.web("#262626"), 50));
        chip.setMinWidth(Label.USE_PREF_SIZE);
        chip.setCursor(Cursor.HAND);
        chip.setOnMouseClicked(e -> {
            filter = which;
            refresh();
        });
        return chip;
    }

    // ----- Feed -----

    private void buildFeed() {
        feed.getChildren().clear();
        List<ShiftRecord> today = matching(filter, OperatorState.getRecords());
        List<ShiftRecord> earlier = matching(filter, OperatorState.getEarlierRecords());

        feed.getChildren().add(sectionTitle(LocalDate.now().format(DAY) + " Shift"));
        if (today.isEmpty()) {
            Label none = CommonUi.small(OperatorState.getRecords().isEmpty()
                    ? "No fares yet this shift. Taps in Ready Mode show up here."
                    : "No fares match this filter.", Theme.GRAY);
            none.setWrapText(true);
            feed.getChildren().add(none);
        }
        for (ShiftRecord record : today) {
            feed.getChildren().add(fareCard(record));
        }

        if (!earlier.isEmpty()) {
            feed.getChildren().add(sectionTitle(SampleShift.DATE + " Shift"));
            for (ShiftRecord record : earlier) {
                feed.getChildren().add(fareCard(record));
            }
        }

        if (OperatorState.pendingRecords() > 0) {
            Label note = CommonUi.small("Pending records are saved on this device.", Theme.WHITE);
            VBox.setMargin(note, new Insets(6, 0, 0, 0));
            Button sync = CommonUi.limeButton("Sync pending records", () -> Navigator.go("operatorSync"));
            sync.setFocusTraversable(false);   // with focus the list would open scrolled to the bottom
            feed.getChildren().addAll(note, sync);
        }
    }

    private List<ShiftRecord> matching(Filter which, List<ShiftRecord> records) {
        Predicate<ShiftRecord> test;
        switch (which) {
            case CARD:
                test = r -> !r.isApp();
                break;
            case APP:
                test = ShiftRecord::isApp;
                break;
            case PENDING:
                test = r -> !r.isSynced();
                break;
            default:
                test = r -> true;
        }
        List<ShiftRecord> list = new ArrayList<>();
        for (ShiftRecord record : records) {
            if (test.test(record)) {
                list.add(record);
            }
        }
        return list;
    }

    // "RECENT SHIFT FEEDS ............ 17 Nov Shift"
    private Node sectionTitle(String day) {
        Label left = CommonUi.smallBold("RECENT SHIFT FEEDS", Theme.WHITE);
        left.setFont(Theme.bold(10));
        Label right = CommonUi.smallBold(day, Theme.WHITE);
        Region fill = new Region();
        HBox.setHgrow(fill, Priority.ALWAYS);
        HBox row = new HBox(8, left, fill, right);
        row.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(row, new Insets(6, 0, 0, 0));
        return row;
    }

    // One fare: icon | ₱13.00 [tag] / route / detail / status   ...   time / Card / ****6767
    private Node fareCard(ShiftRecord r) {
        Label amount = TripUi.bold(OperatorUi.peso(r.getCents()), 16);
        amount.setWrapText(false);
        HBox top = new HBox(8, amount);
        top.setAlignment(Pos.CENTER_LEFT);
        Label tag = tagFor(r);
        if (tag != null) {
            top.getChildren().add(tag);
        }

        Label route = CommonUi.smallBold(r.getFrom() + " › " + r.getTo(), Theme.WHITE);
        route.setFont(Theme.bold(12));
        route.setWrapText(true);
        Label detail = CommonUi.small(detailFor(r), Color.web("#BDBDBD"));
        detail.setWrapText(true);
        VBox middle = new VBox(3, top, route, detail, statusFor(r));
        HBox.setHgrow(middle, Priority.ALWAYS);

        Label time = CommonUi.small(r.getTime().format(TIME), Theme.WHITE);
        VBox right = new VBox(2, time, CommonUi.small(r.isApp() ? "App" : "Card", Theme.WHITE),
                CommonUi.small(r.getWho().replace(" ", ""), Theme.WHITE));
        right.setAlignment(Pos.TOP_RIGHT);
        right.setMinWidth(VBox.USE_PREF_SIZE);

        HBox card = new HBox(10, iconBox(r), middle, right);
        card.setPadding(new Insets(12));
        if (r.isMaxFare()) {
            card.setBackground(Theme.rounded(Color.web("#2A1616"), 10));
            card.setBorder(new Border(new BorderStroke(RED, BorderStrokeStyle.SOLID,
                    new CornerRadii(10), new BorderWidths(1))));
        } else {
            card.setBackground(Theme.rounded(OperatorUi.CARD, 10));
        }
        return card;
    }

    // NFC waves for cards, a QR for app users, a warning sign for a max fare
    private Node iconBox(ShiftRecord r) {
        Node icon;
        if (r.isMaxFare()) {
            Group warning = WarningDialog.warningIcon();
            warning.setScaleX(0.3);
            warning.setScaleY(0.3);
            icon = new Group(warning);
        } else {
            icon = Icons.view(r.isApp() ? "icon_op_app_qr" : "icon_op_nfc", 16);
        }
        StackPane box = new StackPane(icon);
        box.setMinSize(30, 30);
        box.setMaxSize(30, 30);
        box.setBackground(Theme.rounded(Color.web("#141414"), 6));
        return box;
    }

    // "Student -20%" / "Max Fare Flag" / "App QR"
    private Label tagFor(ShiftRecord r) {
        if (r.isMaxFare()) {
            return tag("Max Fare Flag", Theme.WHITE, RED);
        }
        if (r.getDiscountCents() > 0) {
            return tag("Student -20%", Theme.LIME, Color.web("#2C3308"));
        }
        if (r.isApp()) {
            return tag("App QR", Theme.WHITE, Color.web("#333333"));
        }
        return null;
    }

    private Label tag(String text, Color textColor, Color fill) {
        Label tag = CommonUi.smallBold(text, textColor);
        tag.setFont(Theme.bold(8));
        tag.setPadding(new Insets(2, 6, 2, 6));
        tag.setBackground(Theme.rounded(fill, 4));
        tag.setMinWidth(Label.USE_PREF_SIZE);
        return tag;
    }

    // "3.3 km · Student discount applied (-₱3.91) · Base ₱19.57"
    private String detailFor(ShiftRecord r) {
        if (r.isMaxFare()) {
            return "Passenger didn't tap out · Auto penalty applied";
        }
        String km = String.format(Locale.US, "%.1f km", r.getKm());
        if (r.getDiscountCents() > 0) {
            return km + " · Student discount applied (-" + OperatorUi.peso(r.getDiscountCents())
                    + ") · Base " + OperatorUi.peso(r.getBaseCents());
        }
        return km + " · Regular fare";
    }

    // "✓ Server confirmed" / "Pending sync" / "Payment pending" (app fare saved offline)
    private Label statusFor(ShiftRecord r) {
        if (r.isPaymentPending()) {
            return CommonUi.smallBold("Payment pending", OperatorUi.AMBER);
        }
        if (!r.isSynced()) {
            return CommonUi.smallBold("Pending sync", OperatorUi.AMBER);
        }
        return CommonUi.smallBold(r.isApp() ? "Server confirmed" : "Synced", Theme.LIME);
    }
}
