package com.farex.ui.load;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import com.farex.ui.common.Theme;

// Buy Load, step 3 (title "Payment"): the phone, account or card number for the chosen payment method.
// The app checks the number's format (and a card's checksum). Whether the account is really the rider's
// is proven on PayMongo's page, where they log in to GCash / the bank / the card's 3-D Secure.
public class LoadAccountStep extends VBox {

    private final Runnable onDone;

    private static final Border IDLE_BORDER = new Border(new BorderStroke(
            Color.TRANSPARENT, BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(1.5)));
    private static final Border FOCUS_BORDER = new Border(new BorderStroke(
            Theme.LIME, BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(1.5)));
    private static final Border ERROR_BORDER = new Border(new BorderStroke(
            Theme.ERROR, BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(1.5)));

    private final TextField field = new TextField();
    private final Label hint = new Label();
    private final String method = BuyLoadState.getMethod();

    // onDone runs when the step is done (BuyLoadScreen shows the following step)
    public LoadAccountStep(Runnable onDone) {
        super(16);
        VBox.setVgrow(this, Priority.ALWAYS);
        this.onDone = onDone;
        Label caption = new Label(caption(method));
        caption.setTextFill(Theme.WHITE);
        caption.setFont(Font.font(Theme.FONT, 12));

        field.setPromptText(example(method));
        field.setBackground(Theme.rounded(Theme.FIELD, 8));
        field.setBorder(IDLE_BORDER);
        field.setPadding(new Insets(15, 14, 15, 14));
        field.setFont(Font.font(Theme.FONT, 14));
        field.setStyle("-fx-text-fill: white; -fx-prompt-text-fill: #6F6F6F;");
        field.focusedProperty().addListener((obs, was, now) -> field.setBorder(now ? FOCUS_BORDER : IDLE_BORDER));

        // Digits only, shown in groups as typed: "0912 345 6789", "1234 5678 90"
        field.textProperty().addListener((obs, oldText, newText) -> {
            String digits = newText.replaceAll("\\D", "");
            if (digits.length() > maxDigits(method)) {
                digits = digits.substring(0, maxDigits(method));
            }
            String shown = group(digits);
            if (!shown.equals(newText)) {
                field.setText(shown);
                field.positionCaret(shown.length());
                return;
            }
            showHint(digits);
        });

        hint.setWrapText(true);
        showHint("");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button next = new Button("Next");
        Theme.limeButton(next);
        next.setMaxWidth(Double.MAX_VALUE);
        next.setOnAction(e -> goNext());
        field.setOnAction(e -> next.fire());

        getChildren().addAll(LoadMethodStep.badge(method), caption, field, hint, spacer, next);

        if (BuyLoadState.getAccountNumber() != null) {   // coming back from the next step
            field.setText(BuyLoadState.getAccountNumber());
        }
        Platform.runLater(field::requestFocus);
    }

    private void goNext() {
        String digits = field.getText().replaceAll("\\D", "");
        String problem = problem(method, digits);
        if (problem != null) {
            hint.setText(problem);
            hint.setTextFill(Theme.ERROR);
            field.setBorder(ERROR_BORDER);
            return;
        }
        BuyLoadState.setAccountNumber(group(digits));
        onDone.run();
    }

    // Gray rule while typing, lime once the number passes every check
    private void showHint(String digits) {
        boolean ok = problem(method, digits) == null;
        hint.setText(ok ? "Number looks right. You confirm it when you pay." : rule(method));
        hint.setTextFill(ok ? Theme.LIME : Theme.GRAY);
        if (field.isFocused()) {
            field.setBorder(FOCUS_BORDER);
        }
    }

    // ----- Rules per method (static, so they can be checked without a screen) -----

    static int maxDigits(String method) {
        return switch (method) {
            case "GCash" -> 11;
            case "BPI" -> 10;
            case "Others" -> 19;
            default -> 16;
        };
    }

    // null when the number can be right, else what to fix
    static String problem(String method, String digits) {
        if (digits.isEmpty()) {
            return caption(method) + ".";
        }
        if (digits.chars().distinct().count() == 1) {
            return "That number is not real. Check it and try again.";
        }
        switch (method) {
            case "GCash":
                return isMobile(digits) ? null : "Enter the 11-digit mobile number of your GCash, starting with 09.";
            case "BPI":
                return digits.length() == 10 ? null : "A BPI account number has 10 digits.";
            case "Others":
                if (isMobile(digits) || (digits.length() >= 13 && luhn(digits))) {
                    return null;
                }
                return digits.length() >= 13
                        ? "This card number is not valid. Check each digit on your card."
                        : "Enter a 13 to 19-digit card number, or an 11-digit e-wallet number starting with 09.";
            default:
                // shortcut: MariBank's exact length is not published here, so any 10 to 16 digits pass; set it once known
                return digits.length() >= 10 ? null : "Enter your 10 to 16-digit account number.";
        }
    }

    private static boolean isMobile(String digits) {
        return digits.matches("09\\d{9}");
    }

    // The card-number checksum every Visa / Mastercard / JCB card passes: catches a typo in one digit, or two swapped
    static boolean luhn(String digits) {
        int sum = 0;
        for (int i = 0; i < digits.length(); i++) {
            int d = digits.charAt(digits.length() - 1 - i) - '0';
            if (i % 2 == 1) {
                d = d * 2 > 9 ? d * 2 - 9 : d * 2;
            }
            sum += d;
        }
        return sum % 10 == 0;
    }

    // "09123456789" -> "0912 345 6789"; anything else in groups of 4
    static String group(String digits) {
        StringBuilder b = new StringBuilder();
        int[] cuts = isMobilePrefix(digits) ? new int[]{4, 7} : new int[]{4, 8, 12, 16};
        int c = 0;
        for (int i = 0; i < digits.length(); i++) {
            if (c < cuts.length && i == cuts[c]) {
                b.append(' ');
                c++;
            }
            b.append(digits.charAt(i));
        }
        return b.toString();
    }

    private static boolean isMobilePrefix(String digits) {
        return digits.startsWith("09") && digits.length() <= 11;
    }

    private static String caption(String method) {
        return switch (method) {
            case "GCash" -> "Enter your GCash mobile number";
            case "BPI" -> "Enter your BPI account number";
            case "MariBank" -> "Enter your MariBank account number";
            default -> "Enter your card number or e-wallet mobile number";
        };
    }

    private static String example(String method) {
        return switch (method) {
            case "GCash" -> "0912 345 6789";
            case "BPI" -> "1234 5678 90";
            case "MariBank" -> "Account number";
            default -> "Card or mobile number";
        };
    }

    private static String rule(String method) {
        return switch (method) {
            case "GCash" -> "11 digits, starts with 09.";
            case "BPI" -> "10 digits, found on your passbook or BPI app.";
            case "MariBank" -> "10 to 16 digits, found in your MariBank app.";
            default -> "A 13 to 19-digit card number, or an 11-digit e-wallet number starting with 09.";
        };
    }
}
