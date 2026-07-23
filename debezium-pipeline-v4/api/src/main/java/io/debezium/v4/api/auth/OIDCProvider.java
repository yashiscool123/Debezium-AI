package io.debezium.v4.api.auth;

import jakarta.enterprise.context.ApplicationScoped;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class OIDCProvider implements AuthenticationService.SSOProvider {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private String clientId;
    private String clientSecret;
    private String tokenEndpoint;
    private String userInfoEndpoint;
    private String issuer;

    public void configure(String clientId, String clientSecret, String issuer) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.issuer = issuer;
        this.tokenEndpoint = issuer + "/protocol/openid-connect/token";
        this.userInfoEndpoint = issuer + "/protocol/openid-connect/userinfo";
    }

    @Override
    public String name() { return "oidc"; }

    @Override
    public AuthenticationService.SSOAuthenticationResult authenticate(String authCode, String redirectUri) {
        try {
            String creds = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());
            String body = "grant_type=authorization_code&code=" + authCode + "&redirect_uri=" + redirectUri
                + "&client_id=" + clientId + "&client_secret=" + clientSecret;

            var tokenRequest = HttpRequest.newBuilder()
                .uri(URI.create(tokenEndpoint))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", "Basic " + creds)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            var tokenResponse = httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString());
            if (tokenResponse.statusCode() != 200) {
                return new AuthenticationService.SSOAuthenticationResult(false, "Token exchange failed: " + tokenResponse.statusCode(), null, null);
            }

            var tokenJson = mapper.readTree(tokenResponse.body());
            String accessToken = tokenJson.get("access_token").asText();

            var userInfoRequest = HttpRequest.newBuilder()
                .uri(URI.create(userInfoEndpoint))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

            var userInfoResponse = httpClient.send(userInfoRequest, HttpResponse.BodyHandlers.ofString());
            if (userInfoResponse.statusCode() != 200) {
                return new AuthenticationService.SSOAuthenticationResult(false, "UserInfo failed: " + userInfoResponse.statusCode(), null, null);
            }

            var userInfo = mapper.readTree(userInfoResponse.body());
            String email = userInfo.has("email") ? userInfo.get("email").asText() : userInfo.get("sub").asText() + "@idp.local";
            String name = userInfo.has("name") ? userInfo.get("name").asText() : email;

            return new AuthenticationService.SSOAuthenticationResult(true, "Authenticated", email, name);
        } catch (Exception e) {
            return new AuthenticationService.SSOAuthenticationResult(false, "SSO error: " + e.getMessage(), null, null);
        }
    }
}
