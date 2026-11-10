package app.sanctuary.api.admin;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class AdminCrypto {
    private final AdminProperties properties;
    private final SecureRandom random = new SecureRandom();
    public AdminCrypto(AdminProperties properties) { this.properties = properties; }
    public String token() {
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    public static String hash(String value) {
        try {
            return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new IllegalStateException("Unable to hash admin token"); }
    }
    public String seal(String value) {
        try {
            byte[] iv = new byte[12]; random.nextBytes(iv);
            Cipher cipher = cipher(Cipher.ENCRYPT_MODE, iv);
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] output = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, output, 0, iv.length);
            System.arraycopy(encrypted, 0, output, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(output);
        } catch (Exception e) { throw new IllegalStateException("Admin encryption is not configured correctly"); }
    }
    public String open(String value) {
        try {
            byte[] bytes = Base64.getDecoder().decode(value);
            return new String(cipher(Cipher.DECRYPT_MODE, java.util.Arrays.copyOf(bytes, 12))
                .doFinal(java.util.Arrays.copyOfRange(bytes, 12, bytes.length)), StandardCharsets.UTF_8);
        } catch (Exception e) { throw new IllegalArgumentException("Invalid admin challenge"); }
    }
    private Cipher cipher(int mode, byte[] iv) throws Exception {
        byte[] key = Base64.getDecoder().decode(properties.encryptionKey());
        if (key.length != 32) throw new IllegalArgumentException("Expected a 256-bit encryption key");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
        return cipher;
    }
}
