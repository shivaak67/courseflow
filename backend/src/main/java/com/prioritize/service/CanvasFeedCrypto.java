package com.prioritize.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CanvasFeedCrypto {
    private final SecretKeySpec key;
    public CanvasFeedCrypto(@Value("${CANVAS_FEED_KEY:${app.jwt.secret}}") String secret) {
        try {
            key = new SecretKeySpec(MessageDigest.getInstance("SHA-256")
                    .digest(("prioritize/canvas-feed/v1:" + secret).getBytes(StandardCharsets.UTF_8)), "AES");
        } catch (Exception e) { throw new IllegalStateException("Feed encryption unavailable"); }
    }
    public String encrypt(String text) {
        try {
            byte[] nonce = new byte[12]; new SecureRandom().nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, nonce));
            byte[] data = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(nonce) + "." + Base64.getEncoder().encodeToString(data);
        } catch (Exception e) { throw new IllegalStateException("Could not protect the feed link"); }
    }
    public String decrypt(String text) {
        try {
            String[] parts = text.split("\\.");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, Base64.getDecoder().decode(parts[0])));
            return new String(cipher.doFinal(Base64.getDecoder().decode(parts[1])), StandardCharsets.UTF_8);
        } catch (Exception e) { throw new IllegalStateException("Reconnect your Canvas calendar feed in Settings."); }
    }
}
