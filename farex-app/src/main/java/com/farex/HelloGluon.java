package com.farex;

import farex.shared.Enums.PassengerType;
import farex.shared.FareCalculator;
import farex.shared.Model.FareTable;
import java.time.LocalDate;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class HelloGluon extends Application {
    @Override
    public void start(Stage stage) {
        Label out = new Label("FareX");
        Button test = new Button("Test shared: 5 km student fare");
        test.setOnAction(e -> {
            var ft = new FareTable(1300, 150, LocalDate.now());   // centavos
            long fare = FareCalculator.fare(5, PassengerType.STUDENT, ft);
            out.setText(String.format("Fare: P%.2f", fare / 100.0));
        });
        VBox root = new VBox(12, out, test);
        root.setAlignment(Pos.CENTER);
        stage.setScene(new Scene(root, 360, 640));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}