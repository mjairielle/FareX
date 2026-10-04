package farex.app;

import java.net.http.HttpClient;

/** One HTTP wrapper for every screen (passenger, operator, admin). Holds the login token. */
public class ApiClient {
    private final HttpClient http = HttpClient.newHttpClient();
    private final String baseUrl;
    private String token;

    public ApiClient(String baseUrl) { this.baseUrl = baseUrl; }

    public String get(String path) { throw new UnsupportedOperationException("TODO"); }
    public String post(String path, String body) { throw new UnsupportedOperationException("TODO"); }
    public void setToken(String token) { this.token = token; }
}
