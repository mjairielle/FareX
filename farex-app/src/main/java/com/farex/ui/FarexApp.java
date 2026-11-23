package com.farex.ui;

import com.gluonhq.attach.browser.BrowserService;
import com.gluonhq.attach.keyboard.KeyboardService;
import com.gluonhq.attach.util.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ScrollPane;
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
        root.setStyle("-fx-font-family: '" + Theme.FONT + "';");   // text that sets no font of its own: Poppins too
        Navigator.init(root);
        com.farex.ui.common.CardTaps.start();   // real NFC taps on a phone

        // Phone back on the screens that have no back arrow (all others press their arrow)
        Navigator.onBack("splash", Navigator::backToExit);
        Navigator.onBack("welcome", Navigator::backToExit);
        Navigator.onBack("login", () -> Navigator.go("welcome"));
        Navigator.onBack("register", () -> Navigator.go("login"));
        Navigator.onBack("otp", () -> Navigator.go("register"));
        Navigator.onBack("signUpDetails", () -> Navigator.go("register"));   // its "Back" link
        Navigator.onBack("congrats", () -> Navigator.go("login"));           // its button

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
        Navigator.register("scanPay", com.farex.ui.home.ScanPayView::new);       // from Home: pay a jeepney by its QR
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
        scene.setFill(Theme.DARK);              // never a white strip behind the app (keyboard opening, screen changes)
        ignoreTapsAfterScroll(scene);
        // The phone's back button and back swipe reach JavaFX as the Esc key (Gluon maps KEYCODE_BACK to ESCAPE).
        // Phones only: on a laptop Esc stays the demo key for "connection dropped" on the progress screens.
        if (Platform.isAndroid()) {
            scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                    e.consume();
                    Navigator.back();
                }
            });
        }
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

    // While the phone keyboard is up the screen gets only the space above it, like Android's "resize" mode (Messenger):
    // it lays out again with the form right on top of the keyboard, no blank strip and nothing behind the keyboard.
    // If the field being typed in still does not fit, its scroll area scrolls to it; a screen that cannot scroll
    // slides up as a last resort. Nothing moves while the keyboard is closed (a focused field alone is not typing).
    static void keepFieldAboveKeyboard(Scene scene, StackPane root, KeyboardService keyboard) {
        boolean[] pending = {false};
        Runnable reveal = () -> {
            pending[0] = false;
            double kb = keyboard.visibleHeightProperty().get();
            Node field = scene.getFocusOwner();
            double shift = 0;
            if (kb > 0 && field instanceof TextInputControl && field.getScene() != null) {
                double top = scene.getHeight() - kb - 12;                        // what the keyboard leaves visible
                double over = field.localToScene(0, field.getBoundsInLocal().getHeight()).getY() - root.getTranslateY() - top;
                ScrollPane scroll = null;
                for (Node n = field.getParent(); n != null && scroll == null; n = n.getParent()) {
                    if (n instanceof ScrollPane sp) scroll = sp;
                }
                if (over > 0 && scroll != null && scroll.getContent() != null) {
                    double range = scroll.getContent().getLayoutBounds().getHeight() - scroll.getViewportBounds().getHeight();
                    if (range > 0) {
                        double can = (1 - scroll.getVvalue()) * range;            // how far it can still scroll down
                        scroll.setVvalue(Math.min(1, scroll.getVvalue() + Math.min(over, can) / range));
                        over -= Math.min(over, can);
                    }
                }
                shift = Math.max(0, over);
            }
            root.setTranslateY(-shift);
        };
        Runnable later = () -> {                                               // once per pulse, after layout
            if (!pending[0]) {
                pending[0] = true;
                javafx.application.Platform.runLater(reveal);
            }
        };
        keyboard.visibleHeightProperty().addListener((o, old, h) -> {
            root.setPadding(new Insets(0, 0, Math.max(0, h.doubleValue()), 0));
            later.run();
        });
        // The field can move after it got focus (layout, an error line above it, our own scroll): check again
        javafx.beans.value.ChangeListener<Object> moved = (o, was, now) -> later.run();
        scene.focusOwnerProperty().addListener((o, old, focused) -> {
            if (old != null) old.localToSceneTransformProperty().removeListener(moved);
            if (focused instanceof TextInputControl) focused.localToSceneTransformProperty().addListener(moved);
            later.run();
        });
        scene.heightProperty().addListener((o, was, now) -> later.run());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
