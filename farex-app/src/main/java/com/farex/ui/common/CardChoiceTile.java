package com.farex.ui.common;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;

import com.farex.ui.data.DemoCard;

// One square card tile on the Buy Load screen. Lime outline when selected.
public class CardChoiceTile extends VBox {

    private static final double SIZE = 108;
    private static final Border SELECTED = new Border(new BorderStroke(
            Theme.LIME, BorderStrokeStyle.SOLID, new CornerRadii(10), new BorderWidths(2)));
    private static final Border NORMAL = new Border(new BorderStroke(
            Color.TRANSPARENT, BorderStrokeStyle.SOLID, new CornerRadii(10), new BorderWidths(2)));

    private final DemoCard card;

    public CardChoiceTile(DemoCard card) {
        this.card = card;

        Label name = new Label("xPress Card");
        name.setTextFill(Theme.WHITE);
        name.setFont(Theme.bold(13));

        Label number = new Label(card.getCardNumber());
        number.setTextFill(Color.web("#CFCFCF"));
        number.setFont(Font.font(Theme.FONT, 10));

        getChildren().addAll(cardIcon(), name, number);
        setSpacing(2);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(10, 6, 8, 6));
        setMinSize(SIZE, SIZE);
        setPrefSize(SIZE, SIZE);
        setMaxSize(SIZE, SIZE);
        setBackground(Theme.rounded(Color.web("#201F1F"), 10));
        setBorder(NORMAL);
        setCursor(Cursor.HAND);
    }

    public DemoCard getCard() {
        return card;
    }

    public void setSelected(boolean selected) {
        setBorder(selected ? SELECTED : NORMAL);
    }

    // ----- Outline card icon -----

    private Group cardIcon() {
        Rectangle cap = outline(4, 2, 36, 11, 12);       // rounded strip on top
        Rectangle body = outline(4, 16, 36, 22, 9);      // card body
        Rectangle pillLeft = outline(10, 27, 10, 6, 6);  // the two small pills
        Rectangle pillRight = outline(19, 27, 11, 6, 6);
        Group icon = new Group(cap, body, pillLeft, pillRight);
        VBox.setMargin(icon, new Insets(0, 0, 8, 0));
        return icon;
    }

    private Rectangle outline(double x, double y, double w, double h, double arc) {
        Rectangle shape = new Rectangle(x, y, w, h);
        shape.setArcWidth(arc);
        shape.setArcHeight(arc);
        shape.setFill(Color.TRANSPARENT);
        shape.setStroke(Theme.LIME);
        shape.setStrokeWidth(2);
        return shape;
    }
}
