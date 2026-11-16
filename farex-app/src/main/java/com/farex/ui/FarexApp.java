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
        Navigator.register("routes", () -> {          // the main screen, opened on its Routes tab
            MainShell shell = new MainShell();
            shell.showTab(1);
            return shell;
        });
        Navigator.register("tripMap", TripMapView::new);
        Navigator.register("tripNew", () -> {          // from Routes: start a fresh plan
            TripState.clear();
            return new TripPlannerView();
        });
        Navigator.register("tripPlanner", TripPlannerView::new);       // keeps the chosen stops
        Navigator.register("tripStart", () -> new TripPickView(true));
        Navigator.register("tripDestination", () -> new TripPickView(false));
        Navigator.register("tripSearching", TripSearchView::new);
        Navigator.register("tripRoutes", TripRoutesView::new);
        Navigator.register("tripRoute", TripRouteView::new);          // details of the chosen route
        Navigator.register("tripNoRoute", TripNoRouteView::new);
        Navigator.register("tripOffline", TripOfflineView::new);
        Navigator.register("tripLocation", TripLocationView::new);
        Navigator.register("tripNoLocation", TripNoLocationView::new);
        Navigator.register("tripTracking", TripTrackingView::new);
        Navigator.register("tripOffRoute", TripOffRouteView::new);
        Navigator.register("tripEndConfirm", TripEndView::new);
        Navigator.register("tripArrived", TripArrivedView::new);
        Navigator.register("tripRouteMap", TripRouteMapView::new);
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
        Navigator.register("lostCard", LostCardState::start);             // from Home
        Navigator.register("lostCurrent", LostCardState::currentScreen);   // after picking a card
        Navigator.register("lostChoose", LostChooseView::new);
        Navigator.register("lostConfirm", LostConfirmView::new);
        Navigator.register("lostOtp", LostCardOtpView::new);
        Navigator.register("lostProgress", LostProgressView::new);
        Navigator.register("lostReported", LostReportedView::new);
        Navigator.register("lostFailed", LostFailedView::new);
        Navigator.register("lostReplace", LostReplaceView::new);
        Navigator.register("lostLink", LostLinkView::new);
        Navigator.register("lostTransferReview", LostTransferReviewView::new);
        Navigator.register("lostTransfer", LostTransferView::new);
        Navigator.register("lostReplaced", LostReplacedView::new);
        Navigator.register("lostTransferFailed", LostTransferFailedView::new);
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
