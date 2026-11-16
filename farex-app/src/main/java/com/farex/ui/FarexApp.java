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
        Navigator.register("loadAmount", LoadAmountView::new);
        Navigator.register("loadReview", ReviewView::new);
        Navigator.register("loadOtp", LoadOtpView::new);
        Navigator.register("loadReceipt", LoadReceiptView::new);
        Navigator.register("allCards", AllCardsView::new);
        Navigator.register("fetchLoad", FetchLoadState::start);           // from Home
        Navigator.register("fetchCurrent", FetchLoadState::currentScreen); // after picking a card
        Navigator.register("fetchHold", FetchHoldView::new);
        Navigator.register("fetchProgress", FetchProgressView::new);
        Navigator.register("fetchSuccess", FetchSuccessView::new);
        Navigator.register("fetchFailed", FetchFailedView::new);
        Navigator.register("transactions", () -> {   // the main screen, opened on its Transactions tab
            MainShell shell = new MainShell();
            shell.showTab(2);
            return shell;
        });
        Navigator.register("redeem", () -> {           // from Home: start with 100 pts selected
            RedeemState.clear();
            return new RedeemChooseView();
        });
        Navigator.register("redeemChoose", RedeemChooseView::new);   // back to the choice, keeps it
        Navigator.register("redeemReview", RedeemReviewView::new);
        Navigator.register("redeemProgress", RedeemProgressView::new);
        Navigator.register("redeemSuccess", RedeemSuccessView::new);
        Navigator.register("redeemFailed", RedeemFailedView::new);
        Navigator.register("redeemNotEnough", RedeemNotEnoughView::new);
        Navigator.register("lostCard", () -> new PlaceholderView("Replace or Report Lost Card", "dashboard"));
        Navigator.register("notifications", NotificationsView::new);

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
