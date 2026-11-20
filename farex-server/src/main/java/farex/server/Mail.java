package farex.server;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Sends one plain-text email over SMTPS (TLS from the first byte, e.g. smtp.gmail.com port 465).
 * No library: the JDK's SSLSocket speaking SMTP. Settings in .env:
 *   SMTP_USER=team.farex@gmail.com   SMTP_PASSWORD=the 16-letter Gmail App Password
 *   SMTP_HOST / SMTP_PORT only to use another provider (defaults smtp.gmail.com / 465).
 */
final class Mail {
    private Mail() {}

    static boolean configured() {
        return Env.get("SMTP_USER") != null && Env.get("SMTP_PASSWORD") != null;
    }

    static void send(String to, String subject, String text) throws IOException {
        // the address goes into SMTP commands and headers: one line, plain address only
        if (!to.matches("[^\\s,<>@]+@[^\\s,<>@]+")) throw new IllegalArgumentException("Email is not valid");
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
