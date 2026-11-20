package com.farex.ui;

import com.gluonhq.attach.browser.BrowserService;
import com.gluonhq.attach.keyboard.KeyboardService;
import com.gluonhq.attach.util.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.MouseEvent;
import javafx.stage.Screen;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import com.farex.ui.auth.AccountTypeView;
import com.farex.ui.auth.CongratsView;
import com.farex.ui.auth.LoginView;
import com.farex.ui.auth.SignUpDetailsView;
import com.farex.ui.auth.RegisterView;
import com.farex.ui.auth.SplashView;
import com.farex.ui.auth.WelcomeView;
import com.farex.ui.common.Navigator;
import com.farex.ui.common.OtpView;
import com.farex.ui.common.Theme;
import com.farex.ui.fetch.FetchLoadState;
import com.farex.ui.home.AddCardView;
import com.farex.ui.home.AllCardsView;
import com.farex.ui.home.MainShell;
import com.farex.ui.home.NotificationsView;
import com.farex.ui.load.BuyLoadScreen;
import com.farex.ui.lostcard.LostCardState;
import com.farex.ui.operator.OperatorIntroView;
import com.farex.ui.operator.OperatorShell;
import com.farex.ui.operator.OperatorState;
import com.farex.ui.operator.OperatorSummaryView;
import com.farex.ui.operator.OperatorSyncScreen;
import com.farex.ui.profile.AccountScreen;
import com.farex.ui.profile.ChangePasswordView;
import com.farex.ui.profile.DeleteAccountView;
import com.farex.ui.profile.PrivacyView;
import com.farex.ui.profile.TermsView;
import com.farex.ui.redeem.RedeemScreen;
import com.farex.ui.redeem.RedeemState;

public class FarexApp extends Application {

    private static javafx.application.HostServices host;

    // PayMongo checkout page in the browser: the phone's browser through Gluon, the desktop one otherwise
    public static void openUrl(String url) {
        java.util.Optional<BrowserService> phone = BrowserService.create();
        try {
            if (phone.isPresent()) phone.get().launchExternalBrowser(url);
            else if (host != null) host.showDocument(url);
        } catch (Exception e) {
            System.err.println("Could not open " + url + ": " + e.getMessage());
        }
    }

    @Override
    public void start(Stage stage) {
        try {
            host = getHostServices();
        } catch (RuntimeException notOnPhones) {
            host = null;
        }
        Theme.loadFonts();
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
        Navigator.register("signUpDetails", SignUpDetailsView::new);   // name + phone, after the OTP
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
        Navigator.register("chooseRole", () -> new AccountTypeView(true));  // "Log in as" for a passenger + operator account
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
        Navigator.register("redeem", () -> {           // from Home: the whole flow, 100 pts selected
            RedeemState.clear();
            return new RedeemScreen();
        });
        Navigator.register("lostCard", LostCardState::start);             // from Home, the whole flow
        Navigator.register("notifications", NotificationsView::new);

        // Phones: the whole screen (a fixed 360 x 720 left black borders on other screen sizes).
        // Desktop: a phone-sized window.
        Scene scene;
        if (Platform.isDesktop()) {
            scene = new Scene(root, 360, 720);
        } else {
            Rectangle2D screen = Screen.getPrimary().getVisualBounds();
            scene = new Scene(root, screen.getWidth(), screen.getHeight());
        }
        ignoreTapsAfterScroll(scene);
        KeyboardService.create().ifPresent(keyboard -> keepFieldAboveKeyboard(scene, root, keyboard));

        stage.setTitle("FareX");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        Navigator.go("splash");
    }

    // A finger that moved more than a few pixels was scrolling. Where it lifts must not count as a tap:
    // buttons are disarmed before they fire, and the click on tiles and links is dropped.
    static void ignoreTapsAfterScroll(Scene scene) {
        double[] down = new double[2];
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            down[0] = e.getSceneX();
            down[1] = e.getSceneY();
        });
        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            if (!moved(e, down)) return;
            for (Node n = e.getPickResult().getIntersectedNode(); n != null; n = n.getParent()) {
                if (n instanceof ButtonBase button) button.disarm();
            }
        });
        scene.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            if (moved(e, down)) e.consume();
        });
    }

    static boolean moved(MouseEvent e, double[] down) {
        return Math.hypot(e.getSceneX() - down[0], e.getSceneY() - down[1]) > 12;
    }

    // While the phone keyboard is up, slide the whole screen up just enough that the field being typed in
    // stays above it (the keyboard covered the email and password boxes)
    static void keepFieldAboveKeyboard(Scene scene, StackPane root, KeyboardService keyboard) {
        Runnable fit = () -> {
            double keyboardTop = scene.getHeight() - keyboard.visibleHeightProperty().get();
            Node field = scene.getFocusOwner();
            double shift = 0;
            if (keyboard.visibleHeightProperty().get() > 0 && field instanceof TextInputControl) {
                double bottom = field.localToScene(0, field.getBoundsInLocal().getHeight()).getY() - root.getTranslateY();
                shift = Math.max(0, bottom + 16 - keyboardTop);
            }
            root.setTranslateY(-shift);
        };
        keyboard.visibleHeightProperty().addListener((o, old, height) -> fit.run());
        scene.focusOwnerProperty().addListener((o, old, focused) -> fit.run());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
