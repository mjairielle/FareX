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
        Navigator.register("login", () -> {           // passenger login (leaves Operator Mode)
            OperatorState.leave();
            return new LoginView();
        });
        Navigator.register("register", () -> {
            OperatorState.leave();
            return new RegisterView();
        });
        Navigator.register("otp", OtpView::new);
        Navigator.register("accountType", AccountTypeView::new);
        Navigator.register("congrats", CongratsView::new);
        Navigator.register("dashboard", () -> {       // passenger main screen (leaves Operator Mode)
            OperatorState.leave();
            return new MainShell();
        });
        Navigator.register("addCard", AddCardView::new);
        Navigator.register("buyLoad", BuyLoadScreen::new);            // from Home, the whole flow
        Navigator.register("allCards", AllCardsView::new);
        Navigator.register("fetchLoad", FetchLoadState::start);           // from Home, the whole flow
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
        Navigator.register("profile", () -> {         // the main screen, opened on its Profile tab
            OperatorState.leave();
            MainShell shell = new MainShell();
            shell.showTab(3);
            return shell;
        });
        Navigator.register("operatorMode", OperatorIntroView::new);    // "Switch to Operator Mode?"
        Navigator.register("operatorLogin", () -> {
            OperatorState.enter();
            return new LoginView();
        });
        Navigator.register("operatorRegister", () -> {
            OperatorState.enter();
            return new RegisterView();
        });
        Navigator.register("operatorDashboard", () -> new OperatorShell(0));
        Navigator.register("operatorReady", () -> new OperatorShell(1));
        Navigator.register("operatorHistory", () -> new OperatorShell(2));
        Navigator.register("operatorShell", () -> new OperatorShell(OperatorState.getLastTab()));  // last tab shown
        Navigator.register("operatorSummary", OperatorSummaryView::new);
        Navigator.register("operatorSync", OperatorSyncScreen::new);   // ready + uploading, results as pop-ups
        Navigator.register("accountInfo", AccountScreen::new);         // view + edit in one screen
        Navigator.register("privacy", PrivacyView::new);
        Navigator.register("changePassword", ChangePasswordView::new);
        Navigator.register("terms", TermsView::new);
        Navigator.register("deleteAccount", DeleteAccountView::new);
        Navigator.register("tripMap", TripMapView::new);              // Explore map (from Routes)
        Navigator.register("tripNew", () -> {          // from Routes: the whole planner, fresh
            TripState.clear();
            return new TripScreen();
        });
        Navigator.register("redeem", () -> {           // from Home: the whole flow, 100 pts selected
            RedeemState.clear();
            return new RedeemScreen();
        });
        Navigator.register("lostCard", LostCardState::start);             // from Home, the whole flow
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
