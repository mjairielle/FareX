package com.farex.ui.home;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
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
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import com.farex.ui.common.Icons;
import com.farex.ui.common.Theme;
import com.farex.ui.data.DemoAccount;
import com.farex.ui.data.DemoTransaction;
import com.farex.ui.data.MockData;

// Transactions tab (inside MainShell): the history grouped by day, newest first
public class TransactionsView extends ScrollPane {

    // "September 25, 2026"
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US);
    private static final Color GROUP = Color.web("#1C1C1C");
    private static final Color LINE = Color.web("#3A3A3A");

    public TransactionsView() {
        DemoAccount account = MockData.getCurrentAccount();
        List<DemoTransaction> all = (account == null) ? new ArrayList<>() : account.getTransactions();

        VBox content = new VBox(12);
        content.setPadding(new Insets(8, 18, 20, 18));

        if (all.isEmpty()) {
            Label empty = Theme.bodyText("No transactions yet.", Theme.GRAY);
            VBox.setMargin(empty, new Insets(40, 0, 0, 0));
            content.setAlignment(Pos.TOP_CENTER);
            content.getChildren().add(empty);
        } else {
            for (Map.Entry<LocalDate, List<DemoTransaction>> day : groupByDay(all).entrySet()) {
                Label date = new Label(day.getKey().format(DAY));
                date.setTextFill(Theme.WHITE);
                date.setFont(Font.font(Theme.FONT, FontWeight.MEDIUM, 13));
                VBox.setMargin(date, new Insets(6, 0, 0, 0));
                content.getChildren().addAll(date, group(day.getValue()));
            }
        }

        setContent(content);
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setVbarPolicy(ScrollBarPolicy.NEVER);
        setStyle("-fx-background: transparent; -fx-background-color: transparent;");
    }

    // [Sept 25 Buy Load, Sept 25 Pay Fare, Sept 24 Transfer] -> {Sept 25: [2 items], Sept 24: [1 item]}
    private Map<LocalDate, List<DemoTransaction>> groupByDay(List<DemoTransaction> all) {
        Map<LocalDate, List<DemoTransaction>> days = new LinkedHashMap<>();
        for (DemoTransaction t : all) {
            days.computeIfAbsent(t.getDate(), d -> new ArrayList<>()).add(t);
        }
        return days;
    }

    // One rounded box per day, with a thin line between rows
    private Node group(List<DemoTransaction> items) {
        VBox box = new VBox();
        box.setPadding(new Insets(4, 12, 4, 12));
        box.setBackground(Theme.rounded(GROUP, 12));
        box.setBorder(new Border(new BorderStroke(LINE,
                BorderStrokeStyle.SOLID, new CornerRadii(12), new BorderWidths(1))));
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                box.getChildren().add(separator());
            }
            box.getChildren().add(row(items.get(i)));
        }
        return box;
    }

    // icon | "Buy Load" / "3:00 PM | GCash" | "+ ₱ 500.00"
    private Node row(DemoTransaction t) {
        Label title = new Label(t.getTitle());
        title.setTextFill(Theme.WHITE);
        title.setFont(Theme.bold(12));
        Label detail = new Label(t.getDetail());
        detail.setTextFill(Color.web("#CFCFCF"));
        detail.setFont(Font.font(Theme.FONT, 8));
        VBox text = new VBox(1, title, detail);

        Region fill = new Region();
        HBox.setHgrow(fill, Priority.ALWAYS);

        Label amount = new Label(signedPeso(t.getAmount()));
        amount.setTextFill(Theme.WHITE);
        amount.setFont(Font.font(Theme.FONT, 11));

        HBox row = new HBox(14, Icons.view(t.getIconName(), 20), text, fill, amount);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 0, 14, 0));
        return row;
    }

    // 500 -> "+ ₱ 500.00", -35 -> "- ₱ 35.00"
    private String signedPeso(double amount) {
        return String.format(Locale.US, "%s ₱ %,.2f", amount < 0 ? "-" : "+", Math.abs(amount));
    }

    private Region separator() {
        Region line = new Region();
        line.setMinHeight(1);
        line.setPrefHeight(1);
        line.setMaxHeight(1);
        line.setBackground(Theme.solid(LINE));
        return line;
    }
}
