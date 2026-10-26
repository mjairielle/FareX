package com.farex.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class FarexApp extends Application {

    @Override
    public void start(Stage stage) {
        StackPane root = new StackPane();
        root.setBackground(Theme.solid(Theme.DARK));
        Navigator.init(root);

        Navigator.register("splash", SplashView::new);
        Navigator.register("welcome", WelcomeView::new);
        Navigator.register("login", LoginView::new);
        Navigator.register("register", RegisterView::new);
        Navigator.register("otp", OtpView::new);
        Navigator.register("accountType", AccountTypeView::new);
        Navigator.register("congrats", CongratsView::new);
        Navigator.register("dashboard", MainShell::new);
        Navigator.register("addCard", AddCardView::new);
        Navigator.register("buyLoad", BuyLoadView::new);
        Navigator.register("loadPayment", PaymentMethodView::new);
        Navigator.register("loadAccount", AccountNumberView::new);
        Navigator.register("loadAmount", () -> new PlaceholderView("Buy Load", "loadAccount"));
        Navigator.register("allCards", () -> new PlaceholderView("View All Cards", "dashboard"));
        Navigator.register("fetchLoad", () -> new PlaceholderView("Fetch Load", "dashboard"));
        Navigator.register("redeem", () -> new PlaceholderView("Redeem Points", "dashboard"));
        Navigator.register("lostCard", () -> new PlaceholderView("Replace or Report Lost Card", "dashboard"));
        Navigator.register("notifications", () -> new PlaceholderView("Notifications", "dashboard"));

        Scene scene = new Scene(root, 360, 720);

        stage.setTitle("FareX");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        Navigator.go("splash");
    }

    public static void main(String[] args) {
        launch(args);
    }
}