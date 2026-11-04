package com.bcp.workshop.auth;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Map;

public class AuthJwtTestResource implements QuarkusTestResourceLifecycleManager {
    private static PublicKey verificationKey;
    private Path privateKeyPath;

    @Override
    public Map<String, String> start() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            verificationKey = keyPair.getPublic();

            String encodedPrivateKey = Base64.getMimeEncoder(64, new byte[]{'\n'})
                    .encodeToString(keyPair.getPrivate().getEncoded());
            String pemPrivateKey = "-----BEGIN PRIVATE KEY-----\n"
                    + encodedPrivateKey
                    + "\n-----END PRIVATE KEY-----\n";
            privateKeyPath = Files.createTempFile("auth-test-jwt-", ".pem");
            Files.writeString(privateKeyPath, pemPrivateKey);
            return Map.of("smallrye.jwt.sign.key.location", privateKeyPath.toString());
        } catch (Exception error) {
            throw new IllegalStateException("Unable to create ephemeral JWT test key", error);
        }
    }

    @Override
    public void stop() {
        if (privateKeyPath == null) {
            return;
        }
        try {
            Files.deleteIfExists(privateKeyPath);
        } catch (Exception error) {
            throw new IllegalStateException("Unable to remove ephemeral JWT test key", error);
        }
    }

    static PublicKey verificationKey() {
        return verificationKey;
    }
}