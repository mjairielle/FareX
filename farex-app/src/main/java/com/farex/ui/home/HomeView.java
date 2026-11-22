package com.farex.ui.home;

import java.util.Locale;
import java.util.function.IntConsumer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import com.farex.ui.common.Assets;
import com.farex.ui.common.CardTile;
import com.farex.ui.common.Icons;
import com.farex.ui.common.Navigator;
import com.farex.ui.common.Theme;
import com.farex.ui.common.WarningDialog;
import com.farex.ui.data.DemoAccount;
import com.farex.ui.data.DemoCard;
import com.farex.ui.data.MockData;
import com.farex.ui.load.BuyLoadState;

public class HomeView extends ScrollPane {

    private static final Color TILE = Color.web("#1C1C1C");       // tiles and rows
    private static final Color PANEL = Color.web("#222222");      // empty-card panel
    private static final Color BANNER = Color.web("#212121");     // points banner
    private static final Color ICON_TILE = Color.web("#2A2A2A");  // small square behind row icons
    private static final Color LINE = Color.web("#2A2A2A");

    private final IntConsumer showTab;
    private final boolean hasCard;

    public HomeView(IntConsumer showTab) {
        this(showTab, true);
    }

    // Shows the last known data at once; the fresh balance arrives in the background and redraws Home
    private HomeView(IntConsumer showTab, boolean refresh) {
        this.showTab = showTab;

        if (refresh) {
            MockData.refreshInBackground(() -> {
                if (getParent() instanceof Pane parent) {   // still on screen
                    HomeView fresh = new HomeView(showTab, false);
                    fresh.setVvalue(getVvalue());           // keep where the user scrolled to
                    parent.getChildren().setAll(fresh);
                }
            });
        }
        DemoAccount account = MockData.getCurrentAccount();
        DemoCard card = (account == null) ? null : account.getPrimaryCard();
        hasCard = (card != null);
        double points = (account == null) ? 0 : account.getPoints();

        Node cardArea = (card != null) ? new CardTile(card) : emptyCardPanel();

        VBox content = new VBox(16);
        content.setPadding(new Insets(6, 20, 20, 20));
        content.getChildren().addAll(
                greetingRow(account, card),
                cardArea,
                buyLoadButton(),
                actionTiles(),
                divider(),
                pointsBanner(points),
                infoRow("icon_transactions", "View Transactions",
                        "View your fares, loads & transfers", () -> showTab.accept(2)),
                infoRow("icon_lost_card", "Replace or Report Lost Card",
                        "Disable immediately & transfer leftover balance", () -> openCardAction("lostCard")));

        setContent(content);
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setVbarPolicy(ScrollBarPolicy.NEVER);
        // Makes the scroll area see-through (inline style, still Java; no stylesheet file)
        setStyle("-fx-background: transparent; -fx-background-color: transparent;");
    }

    // Greeting + status chip

    // "Aron Ferrancullo" -> "Mabuhay, Aron!" (the profile name, so edits in Account Information show here)
    private Node greetingRow(DemoAccount account, DemoCard card) {
        String name = "";
        if (account != null && !"FareX User".equals(account.getFullName())) {
            name = account.getFullName();
        } else if (card != null) {
            name = card.getHolderName();
        }
        String text = name.trim().isEmpty() ? "Mabuhay!" : "Mabuhay, " + name.trim().split(" ")[0] + "!";
        Label greeting = new Label(text);
        greeting.setTextFill(Theme.WHITE);
        greeting.setFont(Theme.bold(22));
        HBox.setHgrow(greeting, Priority.ALWAYS);
        greeting.setMaxWidth(Double.MAX_VALUE);

        HBox row = new HBox(8, greeting, statusChip(card != null));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(6, 0, 0, 0));
        return row;
    }

    private Node statusChip(boolean hasCard) {
        Circle dot = new Circle(3.5, hasCard ? Theme.LIME : Color.web("#777777"));
        Label label = new Label(hasCard ? "Card Active" : "No Card");
        label.setTextFill(Theme.WHITE);
        label.setFont(Font.font(Theme.FONT, 10));

        HBox chip = new HBox(6, dot, label);
        chip.setAlignment(Pos.CENTER);
        chip.setPadding(new Insets(6, 12, 6, 12));
        chip.setBackground(Theme.rounded(Color.web("#262626"), 50));   // 50 = always a full pill
        return chip;
    }

    // No-card panel

    private Node emptyCardPanel() {
        Label brand = new Label("xPress");
        brand.setTextFill(Theme.WHITE);
        brand.setFont(Theme.bold(20));

        // Plus sign drawn with two thick rounded lines
        Line horizontal = new Line(0, 5.5, 11, 5.5);
        Line vertical = new Line(5.5, 0, 5.5, 11);
        for (Line stroke : new Line[] {horizontal, vertical}) {
            stroke.setStroke(Theme.LIME);
            stroke.setStrokeWidth(2.5);
            stroke.setStrokeLineCap(StrokeLineCap.ROUND);
        }
        Button addCard = new Button("Add Card", new Group(horizontal, vertical));
        addCard.setContentDisplay(ContentDisplay.LEFT);
        addCard.setGraphicTextGap(8);
        addCard.setFont(Theme.bold(13));
        addCard.setPadding(new Insets(8, 16, 8, 16));
        Theme.paintButton(addCard, "#151515", "#1E1E1E", "#0E0E0E", 50, "#CAFE06");
        addCard.setOnAction(e -> Navigator.go("addCard"));

        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox top = new HBox(brand, gap, addCard);
        top.setAlignment(Pos.CENTER_LEFT);

        // Round logo with a lime glow (glow is on the wrapper because a clip would cut it off)
        StackPane logo = new StackPane(Assets.logo(76, 38));
        logo.setEffect(new DropShadow(24, Color.rgb(202, 254, 6, 0.45)));
        logo.setCache(true);                       // the glow drawn once, not on every frame

        Label message = new Label("Add your xPress card to start loading, earning points, "
                + "and tracking your transit activity.");
        message.setTextFill(Color.web("#CFCFCF"));
        message.setFont(Font.font(Theme.FONT, 12));
        message.setWrapText(true);
        message.setTextAlignment(TextAlignment.CENTER);
        message.setAlignment(Pos.CENTER);
        message.setMaxWidth(270);

        VBox panel = new VBox(14, top, logo, message);
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setPadding(new Insets(16, 16, 20, 16));
        panel.setBackground(Theme.rounded(PANEL, 18));
        return panel;
    }

    // Buy Load + the two tiles

    private Node buyLoadButton() {
        Button buy = new Button("BUY LOAD");
        Theme.limeButton(buy);
        buy.setFont(Theme.bold(17));
        buy.setPadding(new Insets(18, 20, 18, 20));
        buy.setMaxWidth(Double.MAX_VALUE);

        // Lime with a slightly darker top edge, like the Figma
        Theme.paintButton(buy,
                "linear-gradient(to bottom, #BFF000, #CAFF06)",
                "linear-gradient(to bottom, #CBFF30, #D6FF55)",
                "linear-gradient(to bottom, #A5CE00, #AFD800)",
                16, "black");

        buy.setOnAction(e -> {
            BuyLoadState.clear();
            openCardAction("buyLoad");
        });
        return buy;
    }

    // Card features need a card. With none, show the Warning instead.
    private void openCardAction(String target) {
        if (hasCard) {
            Navigator.go(target);
        } else {
            Navigator.showOverlay(new WarningDialog());
        }
    }

    private Node actionTiles() {
        return new HBox(12,
                actionTile("icon_view_cards", "View All Cards", "allCards"),
                actionTile("icon_fetch_load", "Fetch Load", "fetchLoad"),
                actionTile("icon_op_qr", "Scan to Pay", "scanPay"));
    }

    private Node actionTile(String iconName, String text, String target) {
        Label label = new Label(text);
        label.setTextFill(Theme.WHITE);
        label.setFont(Theme.bold(13));
        label.setWrapText(true);                                 // three tiles on a narrow phone: "View All / Cards"
        label.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        VBox tile = new VBox(10, Icons.view(iconName, 42), label);
        tile.setAlignment(Pos.CENTER);
        tile.setPadding(new Insets(18, 10, 14, 10));
        tile.setBackground(Theme.rounded(TILE, 14));
        tile.setCursor(Cursor.HAND);
        tile.setPrefWidth(10);        
        tile.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tile, Priority.ALWAYS);
        tile.setOnMouseClicked(e -> openCardAction(target));
        return tile;
    }

    private Node divider() {
        Region line = new Region();
        line.setMinHeight(1);
        line.setPrefHeight(1);
        line.setMaxHeight(1);
        line.setBackground(Theme.solid(LINE));
        VBox.setMargin(line, new Insets(4, 0, 4, 0));
        return line;
    }

    // xPress Points banner

    private Node pointsBanner(double points) {
        String pointsText = String.format(Locale.US, "%05.2f", points);   // 0 -> "00.00"

        StackPane trophyTile = new StackPane(Icons.view("icon_trophy", 30));
        trophyTile.setMinSize(46, 46);
        trophyTile.setPrefSize(46, 46);
        trophyTile.setMaxSize(46, 46);
        trophyTile.setBackground(Theme.rounded(Color.web("#0F0F0F"), 12));

        Label amount = new Label(pointsText);
        amount.setTextFill(Theme.WHITE);
        amount.setFont(Theme.bold(20));
        Label unit = new Label("xPress Points");
        unit.setTextFill(Theme.LIME);
        unit.setFont(Theme.bold(12));
        // Flow + wrapping: on a narrow screen "xPress Points" and the sentence go onto the next line, not "25…"
        javafx.scene.layout.FlowPane topLine = new javafx.scene.layout.FlowPane(6, 0, amount, unit);
        topLine.setAlignment(Pos.BASELINE_LEFT);
        topLine.setPrefWrapLength(120);

        Label sub = new Label("Equivalent to \u20B1" + pointsText + " transit credit");
        sub.setTextFill(Theme.GRAY);
        sub.setFont(Font.font(Theme.FONT, 11));
        sub.setWrapText(true);

        VBox text = new VBox(2, topLine, sub);
        HBox.setHgrow(text, Priority.ALWAYS);

        Button redeem = new Button("Redeem");
        redeem.setFont(Font.font(Theme.FONT, 11));
        redeem.setPadding(new Insets(5, 14, 5, 14));
        Theme.paintButton(redeem, "#363636", "#404040", "#2C2C2C", 50, "#CAFE06");
        redeem.setMinWidth(Region.USE_PREF_SIZE);
        redeem.setOnAction(e -> Navigator.go("redeem"));

        HBox banner = new HBox(12, trophyTile, text, redeem);
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.setPadding(new Insets(12));
        banner.setBackground(Theme.rounded(BANNER, 14));
        banner.setBorder(new Border(new BorderStroke(Color.web("#3A3A3A"),
                BorderStrokeStyle.SOLID, new CornerRadii(14), new BorderWidths(1))));
        return banner;
    }

    // Rows: View Transactions, Replace or Report Lost Card

    private Node infoRow(String iconName, String title, String subtitle, Runnable onClick) {
        StackPane iconTile = new StackPane(Icons.view(iconName, 22));
        iconTile.setMinSize(42, 42);
        iconTile.setPrefSize(42, 42);
        iconTile.setMaxSize(42, 42);
        iconTile.setBackground(Theme.rounded(ICON_TILE, 12));

        Label titleLabel = new Label(title);
        titleLabel.setTextFill(Theme.WHITE);
        titleLabel.setFont(Theme.bold(15));
        Label subLabel = new Label(subtitle);
        subLabel.setTextFill(Theme.GRAY);
        subLabel.setFont(Font.font(Theme.FONT, 11));
        subLabel.setWrapText(true);
        VBox text = new VBox(2, titleLabel, subLabel);
        HBox.setHgrow(text, Priority.ALWAYS);

        Label chevron = new Label("\u203A");
        chevron.setTextFill(Theme.GRAY);
        chevron.setFont(Theme.bold(24));

        HBox row = new HBox(14, iconTile, text, chevron);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14));
        row.setBackground(Theme.rounded(TILE, 14));
        row.setCursor(Cursor.HAND);
        row.setOnMouseClicked(e -> onClick.run());
        return row;
    }
}
