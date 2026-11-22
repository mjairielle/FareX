package com.farex.ui.operator;

import java.util.Map;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

import com.farex.ui.common.Theme;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

// The jeepney's boarding QR in Ready Mode: a real QR code of `text` ("FAREX:JEEP:<operator id>") that the
// passenger's Scan to Pay reads, on a white rounded square with a lime check in the middle. Error
// correction H (30% may be lost) is what lets the check cover the centre and the code still scan.
public class JeepneyQr extends StackPane {

    public JeepneyQr(double size, String text) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(Color.WHITE);
        g.fillRoundRect(0, 0, size, size, 16, 16);

        BitMatrix m;
        try {
            m = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0,
                    Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H, EncodeHintType.MARGIN, 0));
        } catch (WriterException e) {
            throw new IllegalArgumentException("Cannot draw QR for " + text, e);
        }

        // ----- Modules, whole pixels so the edges stay sharp -----
        double padding = size * 0.08;                        // the white quiet zone scanners need
        double cell = Math.floor((size - padding * 2) / m.getWidth());
        double offset = (size - cell * m.getWidth()) / 2;
        g.setFill(Color.web("#141414"));
        for (int y = 0; y < m.getHeight(); y++) {
            for (int x = 0; x < m.getWidth(); x++) {
                if (m.get(x, y)) {
                    g.fillRect(offset + x * cell, offset + y * cell, cell, cell);
                }
            }
        }

        // ----- Lime check in the middle: about 4% of the code, well within what H recovers -----
        double box = cell * m.getWidth() * 0.2;
        double bx = size / 2 - box / 2;
        double by = size / 2 - box / 2;
        g.setFill(Color.WHITE);
        g.fillRoundRect(bx - cell / 2, by - cell / 2, box + cell, box + cell, 8, 8);
        g.setFill(Theme.LIME);
        g.fillRoundRect(bx, by, box, box, 6, 6);
        g.setStroke(Color.web("#141414"));
        g.setLineWidth(box * 0.12);
        g.setLineCap(StrokeLineCap.ROUND);
        g.setLineJoin(StrokeLineJoin.ROUND);
        g.strokePolyline(
                new double[] {bx + box * 0.25, bx + box * 0.43, bx + box * 0.76},
                new double[] {by + box * 0.52, by + box * 0.7, by + box * 0.32}, 3);

        getChildren().add(canvas);
        setMaxSize(size, size);
    }
}
