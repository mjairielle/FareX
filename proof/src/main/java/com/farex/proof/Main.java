package com.farex.proof;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import javafx.application.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/** Proof app: native build runs, and the phone reaches the laptop server over plain http with HttpClient. */
public class Main extends Application {
    @Override
    public void start(Stage stage) {
        TextField url = new TextField("http://localhost:8080/admin");
        Label out = new Label("Press Test");
        out.setWrapText(true);
        Button go = new Button("Test");
        go.setOnAction(e -> new Thread(() -> {
            String r;
            try {
                HttpResponse<String> res = HttpClient.newHttpClient().send(
                        HttpRequest.newBuilder(URI.create(url.getText().trim())).timeout(Duration.ofSeconds(5)).build(),
                        HttpResponse.BodyHandlers.ofString());
                r = "HTTP " + res.statusCode();
            } catch (Exception ex) { r = ex.toString(); }
            String shown = r;
            Platform.runLater(() -> out.setText(shown));
        }).start());
        stage.setScene(new Scene(new VBox(10, url, go, out), 360, 640));
        stage.show();
    }

    public static void main(String[] a) { launch(a); }
}
