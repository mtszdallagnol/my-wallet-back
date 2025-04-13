package General;

import Server.WebServer;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class JWTUtil {
    private static final String HMAC_ALGO = "HmacSHA256";
    public static final long ACCESS_TOKEN_EXPIRATION_TIME = 30 * 60;
    public static final long REFRESH_TOKEN_EXPIRATION_TIME = 1 * 7 * 24 * 60 * 60;

    public static String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    public static String generateToken(String headerJson, String payloadJson) throws NoSuchAlgorithmException, InvalidKeyException {
        String headerEncoded = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));
        String payloadEncoded = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));
        String message = headerEncoded + "." + payloadEncoded;

        SecretKeySpec keySpec = new SecretKeySpec(WebServer.SECRET.getBytes(StandardCharsets.UTF_8), HMAC_ALGO);
        Mac mac = Mac.getInstance(HMAC_ALGO);
        mac.init(keySpec);
        byte[] signatureBytes = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        String signatureEncoded = base64UrlEncode(signatureBytes);

        return message + "." + signatureEncoded;
    }

    public static String generateAccessToken(String subject) throws NoSuchAlgorithmException, InvalidKeyException{
        long currentTime = System.currentTimeMillis() / 1000;
        long expiration = currentTime + ACCESS_TOKEN_EXPIRATION_TIME;

        String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payload = "{\"sub\":\"" + subject + "\",\"exp\":" + expiration + "}";

        return generateToken(header, payload);
    }

    public static String generateRefreshToken(String subject) throws NoSuchAlgorithmException, InvalidKeyException {
        long currentTime = System.currentTimeMillis() / 1000;
        long expiration = currentTime + REFRESH_TOKEN_EXPIRATION_TIME;

        String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payload = "{\"sub\":\"" + subject + "\",\"exp\":" + expiration + ",\"type\":\"refresh\"}";

        return generateToken(header, payload);
    }
}
