package com.bcp.workshop.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;

@ApplicationScoped
public class TokenService {
    public static final int EXPIRES_IN_SECONDS = 3600;
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String HEADER = encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");

    private final byte[] signingKey;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Inject
    public TokenService(@ConfigProperty(name = "auth.token.secret") String secret, ObjectMapper objectMapper) {
        this(secret, objectMapper, Clock.systemUTC());
    }

    TokenService(String secret, ObjectMapper objectMapper, Clock clock) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("Token signing key must contain at least 32 characters");
        }
        this.signingKey = secret.getBytes(StandardCharsets.UTF_8);
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public String create(String username, String role) {
        long issuedAt = clock.instant().getEpochSecond();
        Payload payload = new Payload(username, role, issuedAt, issuedAt + EXPIRES_IN_SECONDS);
        String unsignedToken = HEADER + "." + encodeJson(payload);
        return unsignedToken + "." + encode(sign(unsignedToken));
    }

    public TokenClaims verify(String token) {
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3) {
            throw new SecurityException("Invalid token");
        }
        String unsignedToken = parts[0] + "." + parts[1];
        byte[] expectedSignature = sign(unsignedToken);
        byte[] actualSignature = decode(parts[2]);
        if (!MessageDigest.isEqual(expectedSignature, actualSignature)) {
            throw new SecurityException("Invalid token signature");
        }
        Payload payload = decodeJson(parts[1]);
        if (payload.exp() <= clock.instant().getEpochSecond()) {
            throw new SecurityException("Expired token");
        }
        return new TokenClaims(payload.sub(), payload.role(), payload.iat(), payload.exp());
    }

    private byte[] sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(signingKey, HMAC_ALGORITHM));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException error) {
            throw new IllegalStateException("Token signing is unavailable", error);
        }
    }

    private String encodeJson(Payload payload) {
        try {
            return encode(objectMapper.writeValueAsBytes(payload));
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("Token claims cannot be serialized", error);
        }
    }

    private Payload decodeJson(String encodedPayload) {
        try {
            return objectMapper.readValue(decode(encodedPayload), Payload.class);
        } catch (Exception error) {
            throw new SecurityException("Invalid token claims", error);
        }
    }

    private static String encode(String value) {
        return encode(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private static byte[] decode(String value) {
        try {
            return Base64.getUrlDecoder().decode(value);
        } catch (IllegalArgumentException error) {
            throw new SecurityException("Invalid token encoding", error);
        }
    }

    private record Payload(String sub, String role, long iat, long exp) {}

    public record TokenClaims(String username, String role, long issuedAt, long expiresAt) {}
}
