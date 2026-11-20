package com.farex.ui.operator;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

import com.farex.ui.common.Theme;

// The jeepney's boarding QR in Ready Mode: a white rounded square with a simple 9 x 9 pattern
// (three corner squares like a real QR) and a lime check in the middle.
// It is a picture only; app users "scan" it through the Simulate tap list (Part 26).
public class JeepneyQr extends StackPane {

    // 1 = black block. The three corners are the finder squares; the middle 3 x 3 stays empty for the check.
    private static final int[][] PATTERN = {
        {1, 1, 1, 0, 1, 0, 1, 1, 1},
        {1, 0, 1, 0, 0, 1, 1, 0, 1},
        {1, 1, 1, 0, 1, 0, 1, 1, 1},
        {0, 0, 0, 0, 0, 0, 0, 0, 0},
        {1, 0, 1, 0, 0, 0, 1, 1, 0},
        {0, 0, 0, 0, 0, 0, 0, 1, 1},
        {1, 1, 1, 0, 1, 1, 0, 1, 0},
        {1, 0, 1, 0, 0, 1, 1, 0, 1},
        {1, 1, 1, 0, 1, 0, 1, 1, 1},
    };

    public JeepneyQr(double size) {
        double padding = size * 0.1;
        double cell = (size - padding * 2) / 9;

        Canvas canvas = new Canvas(size, size);
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(Color.WHITE);
        g.fillRoundRect(0, 0, size, size, 16, 16);

        // ----- Blocks -----
        g.setFill(Color.web("#141414"));
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                if (PATTERN[row][col] == 1) {
                    g.fillRect(padding + col * cell, padding + row * cell, cell, cell);
                }
            }
        }

        // ----- Lime check in the middle -----
        double box = cell * 1.8;
        double x = size / 2 - box / 2;
        double y = size / 2 - box / 2;
        g.setFill(Theme.LIME);
        g.fillRoundRect(x, y, box, box, 6, 6);
        g.setStroke(Color.web("#141414"));
        g.setLineWidth(cell * 0.28);
        g.setLineCap(StrokeLineCap.ROUND);
        g.setLineJoin(StrokeLineJoin.ROUND);
        g.strokePolyline(
                new double[] {x + box * 0.25, x + box * 0.43, x + box * 0.76},
                new double[] {y + box * 0.52, y + box * 0.7, y + box * 0.32}, 3);

        getChildren().add(canvas);
        setMaxSize(size, size);
    }
}
