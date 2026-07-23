package io.debezium.v4.api.auth;

import jakarta.enterprise.context.ApplicationScoped;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class AuthenticationService {

    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, UserSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, SSOProvider> ssoProviders = new ConcurrentHashMap<>();
    private final Map<String, String> serviceApiKeys = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public AuthenticationService() {
        var admin = User.builder()
            .id(UUID.randomUUID().toString())
            .username("admin")
            .email("admin@debezium.ai")
            .passwordHash(hashPassword("admin"))
            .type(UserType.HUMAN)
            .addRole(Role.SUPER_ADMIN)
            .tenantId("default")
            .build();
        users.put(admin.username(), admin);
    }

    public record UserSession(String sessionId, String userId, String username, UserType userType,
        Set<Role> roles, Set<Permission> permissions, String tenantId,
        Instant createdAt, Instant expiresAt) {
        public boolean isExpired() { return Instant.now().isAfter(expiresAt); }
    }

    public record AuthResult(boolean success, String message, String sessionId, User user) {}

    public AuthResult login(String username, String password) {
        User user = users.get(username);
        if (user == null) return new AuthResult(false, "Invalid credentials", null, null);
        if (!user.enabled()) return new AuthResult(false, "Account disabled", null, null);
        if (user.type() == UserType.SERVICE) return new AuthResult(false, "Service users must use API key authentication", null, null);

        if (!verifyPassword(password, user.passwordHash())) return new AuthResult(false, "Invalid credentials", null, null);

        String sessionId = UUID.randomUUID().toString();
        var session = new UserSession(sessionId, user.id(), user.username(), user.type(),
            user.roles(), user.permissions(), user.tenantId(),
            Instant.now(), Instant.now().plusSeconds(86400));
        sessions.put(sessionId, session);
        var updated = User.builder()
            .id(user.id()).username(user.username()).email(user.email())
            .passwordHash(user.passwordHash()).type(user.type())
            .roles(user.roles()).permissions(user.permissions())
            .tenantId(user.tenantId()).enabled(user.enabled()).mfaEnabled(user.mfaEnabled())
            .lastLogin(Instant.now()).createdAt(user.createdAt()).updatedAt(Instant.now())
            .attributes(user.attributes()).build();
        users.put(username, updated);
        return new AuthResult(true, "Login successful", sessionId, updated);
    }

    public AuthResult authenticateWithApiKey(String apiKey) {
        String username = serviceApiKeys.get(apiKey);
        if (username == null) return new AuthResult(false, "Invalid API key", null, null);

        User user = users.get(username);
        if (user == null) return new AuthResult(false, "Service user not found", null, null);
        if (!user.enabled()) return new AuthResult(false, "Service account disabled", null, null);

        String sessionId = UUID.randomUUID().toString();
        var session = new UserSession(sessionId, user.id(), user.username(), user.type(),
            user.roles(), user.permissions(), user.tenantId(),
            Instant.now(), Instant.now().plusSeconds(86400));
        sessions.put(sessionId, session);
        var updated = User.builder()
            .id(user.id()).username(user.username()).email(user.email())
            .passwordHash(user.passwordHash()).type(user.type())
            .roles(user.roles()).permissions(user.permissions())
            .tenantId(user.tenantId()).enabled(user.enabled()).mfaEnabled(user.mfaEnabled())
            .lastLogin(Instant.now()).createdAt(user.createdAt()).updatedAt(Instant.now())
            .attributes(user.attributes()).build();
        users.put(username, updated);
        return new AuthResult(true, "API key authentication successful", sessionId, updated);
    }

    public String generateApiKey(String username) {
        User user = users.get(username);
        if (user == null) throw new IllegalArgumentException("User not found: " + username);
        if (user.type() != UserType.SERVICE) throw new IllegalArgumentException("Only service users can have API keys");

        byte[] keyBytes = new byte[32];
        random.nextBytes(keyBytes);
        String apiKey = "dbz_" + Base64.getUrlEncoder().withoutPadding().encodeToString(keyBytes);
        serviceApiKeys.put(apiKey, username);
        return apiKey;
    }

    public void revokeApiKey(String apiKey) {
        serviceApiKeys.remove(apiKey);
    }

    public List<String> listApiKeys(String username) {
        return serviceApiKeys.entrySet().stream()
            .filter(e -> e.getValue().equals(username))
            .map(Map.Entry::getKey)
            .toList();
    }

    public void logout(String sessionId) { sessions.remove(sessionId); }

    public Optional<UserSession> validateSession(String sessionId) {
        var session = sessions.get(sessionId);
        if (session == null || session.isExpired()) {
            if (session != null) sessions.remove(sessionId);
            return Optional.empty();
        }
        return Optional.of(session);
    }

    public boolean hasPermission(String sessionId, Permission permission) {
        return validateSession(sessionId)
            .map(s -> s.permissions().contains(permission) || s.permissions().contains(Permission.ALL))
            .orElse(false);
    }

    public boolean hasRole(String sessionId, Role role) {
        return validateSession(sessionId)
            .map(s -> s.roles().contains(role))
            .orElse(false);
    }

    public User createUser(User user) {
        users.put(user.username(), user);
        return user;
    }

    public Optional<User> getUser(String username) { return Optional.ofNullable(users.get(username)); }
    public List<User> listUsers() { return List.copyOf(users.values()); }
    public List<User> listServiceUsers() {
        return users.values().stream()
            .filter(u -> u.type() == UserType.SERVICE)
            .toList();
    }
    public User updateUser(String username, User user) { users.put(username, user); return user; }
    public User deleteUser(String username) {
        serviceApiKeys.values().removeIf(v -> v.equals(username));
        return users.remove(username);
    }

    // SSO
    public void registerSSOProvider(String name, SSOProvider provider) { ssoProviders.put(name, provider); }

    public record SSOAuthRequest(String provider, String authCode, String redirectUri) {}
    public record SSOAuthResult(boolean success, String message, String sessionId, User user, String provider) {}

    public SSOAuthResult ssoLogin(SSOAuthRequest request) {
        var provider = ssoProviders.get(request.provider());
        if (provider == null) return new SSOAuthResult(false, "Unsupported SSO provider", null, null, request.provider());

        var result = provider.authenticate(request.authCode(), request.redirectUri());
        if (!result.success()) return new SSOAuthResult(false, result.message(), null, null, request.provider());

        User user = users.values().stream()
            .filter(u -> u.email().equalsIgnoreCase(result.email()))
            .findFirst().orElseGet(() -> {
                var newUser = User.builder()
                    .id(UUID.randomUUID().toString()).username(result.email().split("@")[0])
                    .email(result.email()).passwordHash("")
                    .type(UserType.HUMAN)
                    .addRole(Role.PIPELINE_VIEWER).tenantId("default").enabled(true).build();
                users.put(newUser.username(), newUser);
                return newUser;
            });

        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, new UserSession(sessionId, user.id(), user.username(), user.type(),
            user.roles(), user.permissions(), user.tenantId(),
            Instant.now(), Instant.now().plusSeconds(86400)));
        return new SSOAuthResult(true, "SSO login successful", sessionId, user, request.provider());
    }

    public String hashPassword(String password) {
        try {
            byte[] salt = new byte[16];
            random.nextBytes(salt);
            var spec = new PBEKeySpec(password.toCharArray(), salt, 310000, 256);
            var factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) { throw new RuntimeException("Password hashing failed", e); }
    }

    private boolean verifyPassword(String password, String storedHash) {
        try {
            String[] parts = storedHash.split(":");
            if (parts.length != 2) return false;
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[1]);
            var spec = new PBEKeySpec(password.toCharArray(), salt, 310000, 256);
            var factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] actualHash = factory.generateSecret(spec).getEncoded();
            return MessageDigest.isEqual(actualHash, expectedHash);
        } catch (Exception e) { return false; }
    }

    public interface SSOProvider {
        String name();
        SSOAuthenticationResult authenticate(String authCode, String redirectUri);
    }

    public record SSOAuthenticationResult(boolean success, String message, String email, String name) {}
}
