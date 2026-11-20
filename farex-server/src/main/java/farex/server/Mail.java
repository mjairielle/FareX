package farex.server;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Sends one plain-text email. Two ways, set in .env:
 *   Brevo (HTTPS, works on Render's free plan, which blocks mail ports):
 *     BREVO_API_KEY=xkeysib-...   MAIL_FROM=the sender address verified in Brevo
 *   Gmail SMTPS (laptop or a paid server): TLS from the first byte to smtp.gmail.com port 465
 *     SMTP_USER=team.farex@gmail.com   SMTP_PASSWORD=the 16-letter Gmail App Password
 *     SMTP_HOST / SMTP_PORT only to use another provider.
 * No library: the JDK's HttpClient and SSLSocket. Brevo wins when both are set.
 */
final class Mail {
    private Mail() {}

    static boolean configured() {
        return brevo() || (Env.get("SMTP_USER") != null && Env.get("SMTP_PASSWORD") != null);
    }

    private static boolean brevo() {
        return Env.get("BREVO_API_KEY") != null && Env.get("MAIL_FROM") != null;
    }

    static void send(String to, String subject, String text) throws IOException {
        // the address goes into SMTP commands, headers and JSON: one line, plain address only
        if (!to.matches("[^\\s,<>@\"\\\\]+@[^\\s,<>@\"\\\\]+")) throw new IllegalArgumentException("Email is not valid");
        if (brevo()) {
            sendBrevo(to, subject, text);
            return;
        }
        String user = Env.get("SMTP_USER"), password = Env.get("SMTP_PASSWORD").replace(" ", "");
        String host = Env.get("SMTP_HOST", "smtp.gmail.com");
        int port = Integer.parseInt(Env.get("SMTP_PORT", "465"));
        try (SSLSocket s = (SSLSocket) SSLSocketFactory.getDefault().createSocket(host, port)) {
            s.setSoTimeout(20_000);
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            Writer out = new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8);
            expect(in, "220");
            say(out, in, "EHLO farex", "250");
            say(out, in, "AUTH LOGIN", "334");
            say(out, in, b64(user), "334");
            say(out, in, b64(password), "235");
            say(out, in, "MAIL FROM:<" + user + ">", "250");
            say(out, in, "RCPT TO:<" + to + ">", "250");
            say(out, in, "DATA", "354");
            String body = text.replace("\r", "").replace("\n", "\r\n").replace("\r\n.", "\r\n..");   // a lone "." would end the mail
            out.write("From: FareX <" + user + ">\r\nTo: <" + to + ">\r\nSubject: " + subject + "\r\n"
                    + "Date: " + ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME) + "\r\n"
                    + "MIME-Version: 1.0\r\nContent-Type: text/plain; charset=UTF-8\r\n\r\n" + body + "\r\n.\r\n");
            out.flush();
            expect(in, "250");
            say(out, in, "QUIT", "221");
        }
    }

    // Brevo's transactional email API: one JSON POST over HTTPS
    private static void sendBrevo(String to, String subject, String text) throws IOException {
        String body = "{\"sender\":{\"name\":\"FareX\",\"email\":" + json(Env.get("MAIL_FROM")) + "},\"to\":[{\"email\":" + json(to)
                + "}],\"subject\":" + json(subject) + ",\"textContent\":" + json(text) + "}";
        java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder(java.net.URI.create(Env.get("BREVO_URL", "https://api.brevo.com/v3/smtp/email")))
                .timeout(java.time.Duration.ofSeconds(20))
                .header("api-key", Env.get("BREVO_API_KEY")).header("Content-Type", "application/json").header("Accept", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
        try {
            java.net.http.HttpResponse<String> r = java.net.http.HttpClient.newHttpClient().send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() >= 300) throw new IOException("Brevo said " + r.statusCode() + ": " + r.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while sending");
        }
    }

    private static String json(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "") + "\"";
    }

    private static void say(Writer out, BufferedReader in, String line, String code) throws IOException {
        out.write(line + "\r\n");
        out.flush();
        expect(in, code);
    }

    // A reply can span lines ("250-..." then "250 ..."); the last one carries the result
    private static void expect(BufferedReader in, String code) throws IOException {
        String line;
        do {
            line = in.readLine();
            if (line == null) throw new IOException("Mail server closed the connection");
        } while (line.length() > 3 && line.charAt(3) == '-');
        if (!line.startsWith(code)) throw new IOException("Mail server said: " + line);
    }

    private static String b64(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }
}
