package com.farex.ui;

import java.net.URL;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

// The Manila map. Uses images/map_manila.png when it exists; until then it draws a simple
// placeholder map (roads, a park, route badges, "Manila"). Everything is clipped to this pane,
// so it works both as a full background and as a small rounded card.
public class MapView extends Pane {

    private static final String IMAGE = "images/map_manila.png";
    private static final double ART_WIDTH = 360;   // the drawing is made for the phone width

    private ImageView photo;   // the map picture, when map_manila.png exists

    // Full background: grows to fill its parent
    public MapView() {
        this(0);
    }

    // radius > 0 -> rounded corners, e.g. the Explore map card
    public MapView(double radius) {
        URL url = MapView.class.getResource(IMAGE);
        if (url != null) {
            photo = new ImageView(new Image(url.toExternalForm()));
            photo.setPreserveRatio(true);
            photo.setSmooth(true);
            getChildren().add(photo);
        } else {
            getChildren().add(placeholder());
        }

        // Clip to the pane's current size, with rounded corners if asked
        Rectangle clip = new Rectangle();
        clip.setArcWidth(radius * 2);
        clip.setArcHeight(radius * 2);
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        setClip(clip);
        setMinSize(0, 0);
        setPrefSize(0, 0);   // takes the space it is given instead of asking for 360 x 720
    }

    // Fixed size, e.g. new MapView(320, 250, 12) for a card
    public MapView(double width, double height, double radius) {
        this(radius);
        setPrefSize(width, height);
        setMinSize(width, height);
        setMaxSize(width, height);
    }

    // ----- Picture sizing -----

    // "Cover": grow the picture until it fills the whole area, centered; extra edges are clipped.
    // Example: 403 x 610 picture in a 360 x 640 area -> scale = max(360/403, 640/610) = 1.05
    // -> drawn 423 x 640, moved 31 px left so the middle shows.
    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        if (photo == null || photo.getImage() == null) {
            return;
        }
        double w = getWidth();
        double h = getHeight();
        double imageW = photo.getImage().getWidth();
        double imageH = photo.getImage().getHeight();
        if (w <= 0 || h <= 0 || imageW <= 0 || imageH <= 0) {
            return;
        }
        double scale = Math.max(w / imageW, h / imageH);
        photo.setFitWidth(imageW * scale);
        photo.setFitHeight(imageH * scale);
        photo.relocate((w - imageW * scale) / 2, (h - imageH * scale) / 2);
    }

    // ----- Placeholder drawing (on a 360 x 720 area) -----

    private Pane placeholder() {
        Rectangle land = new Rectangle(ART_WIDTH, 720, Color.web("#E9ECEF"));

        Polygon park = new Polygon(80, 60, 150, 50, 165, 150, 120, 175, 70, 140);
        park.setFill(Color.web("#CFE6C8"));
        Polygon water = new Polygon(0, 600, 120, 560, 240, 620, 360, 590, 360, 720, 0, 720);
        water.setFill(Color.web("#C4DDF4"));

        Pane art = new Pane(land, park, water,
                road(10, 0, 40, 180, 30, 420, 60, 720),
                road(0, 260, 140, 300, 260, 250, 360, 270),
                road(360, 120, 250, 200, 170, 330, 120, 480, 90, 720),
                road(200, 0, 230, 160, 300, 330, 360, 400),
                road(0, 470, 130, 460, 260, 520, 360, 500),
                road(150, 720, 190, 560, 260, 430, 360, 360),
                badge("150", 50, 300), badge("170", 105, 395), badge("R-7", 220, 270),
                badge("R-6", 285, 450), badge("R-9", 60, 120),
                cityName());
        art.setMouseTransparent(true);
        return art;
    }

    // A blue-gray road with a lighter center line
    private Pane road(double... points) {
        Polyline edge = new Polyline(points);
        edge.setStroke(Color.web("#9FB0C8"));
        edge.setStrokeWidth(7);
        Polyline center = new Polyline(points);
        center.setStroke(Color.web("#C9D4E3"));
        center.setStrokeWidth(3);
        for (Polyline line : new Polyline[] {edge, center}) {
            line.setFill(null);
            line.setStrokeLineCap(StrokeLineCap.ROUND);
            line.setStrokeLineJoin(StrokeLineJoin.ROUND);
        }
        return new Pane(edge, center);
    }

    // Small green route sign like "150"
    private StackPane badge(String text, double x, double y) {
        Label label = new Label(text);
        label.setTextFill(Color.WHITE);
        label.setFont(Font.font("System", FontWeight.BOLD, 9));
        StackPane badge = new StackPane(label);
        badge.setAlignment(Pos.CENTER);
        badge.setStyle("-fx-background-color: #6D9F45; -fx-background-radius: 3; "
                + "-fx-border-color: white; -fx-border-width: 1.5; -fx-border-radius: 3; -fx-padding: 1 4 1 4;");
        badge.relocate(x, y);
        return badge;
    }

    private Label cityName() {
        Label manila = new Label("Manila");
        manila.setTextFill(Color.web("#3C4043"));
        manila.setFont(Font.font("System", FontWeight.MEDIUM, 28));
        manila.relocate(18, 410);
        return manila;
    }
}
