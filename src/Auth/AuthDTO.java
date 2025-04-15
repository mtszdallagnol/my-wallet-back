package Auth;

import Exceptions.MappingException;
import General.CryptoUtils;
import General.JsonParsers;
import Server.WebServer;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuthDTO {
    private static final long ACCESS_TOKEN_EXPIRY = 2 * 60 * 60;
    private static final long REFRESH_TOKEN_EXPIRY = 1 * 7 * 24 * 60 * 60;

     public enum tokenType {
        ACCESS, REFRESH
    }

    private static class JwtToken {
        private final String header;
        private final String payload;
        private final String signature;

        private JwtToken(String header, String payload, String signature) {
            this.header = header;
            this.payload = payload;
            this.signature = signature;
        }

        public static JwtToken createJwtToken(tokenType type, int userID) throws MappingException, NoSuchAlgorithmException, InvalidKeyException {
            Map<String, String> headerClaims = new HashMap<>();

            headerClaims.put("alg", "HS256");
            headerClaims.put("typ", "JWT");

            JsonParsers.SerializationResult headerJSONResult = JsonParsers.serialize(headerClaims);
            if (!headerJSONResult.isSuccess()) {
                throw new MappingException("Falha ao mapear objeto(s)" + headerJSONResult.getError().getMessage(), List.of());
            }

            String headerJson = headerJSONResult.getJsonString();
            String encodedHeader = Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));

            Map<String, Object> payloadClaims = new HashMap<>();

            if (type == tokenType.ACCESS) payloadClaims.put("sub", String.valueOf(userID));
            payloadClaims.put("iat", Instant.now().getEpochSecond());
            payloadClaims.put("exp", type == tokenType.ACCESS ? Instant.now().plusSeconds(ACCESS_TOKEN_EXPIRY).getEpochSecond()
                    : Instant.now().plusSeconds(REFRESH_TOKEN_EXPIRY).getEpochSecond());
            payloadClaims.put("type", type.toString());

            JsonParsers.SerializationResult payloadJSONResult = JsonParsers.serialize(payloadClaims);
            if (!payloadJSONResult.isSuccess()) {
                throw new MappingException("Falha ao mapear objeto(s)" + payloadJSONResult.getError().getMessage(), List.of());
            }

            String payloadJson = payloadJSONResult.getJsonString();
            String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

            String dataToSign = encodedHeader + "." + encodedPayload;
            String signature = CryptoUtils.generateSHA256Signature(dataToSign, WebServer.JWT_SECRET_KEY);

            return new JwtToken(encodedHeader, encodedPayload, signature);
        }

        public static Object verifyJwtToken(String token) throws NoSuchAlgorithmException, InvalidKeyException, MappingException {
            String[] parts = token.split("\\.");

            if (parts.length != 3) {
                return false;
            }

            String headerEncoded = parts[0];
            String payloadEncoded = parts[1];
            String providedSignature = parts[2];

            String dataToSign = headerEncoded + "." + payloadEncoded;
            String expectedSignature = CryptoUtils.generateSHA256Signature(dataToSign, WebServer.JWT_SECRET_KEY);

            if (!expectedSignature.equals(providedSignature)) {
                return false;
            }

            String payload = new String(Base64.getDecoder().decode(payloadEncoded), StandardCharsets.UTF_8);
            JsonParsers.DeserializationResult<Map<String, Object>> payloadJSONResult = JsonParsers.deserialize(payload);

            if (!payloadJSONResult.isSuccess()) {
                throw new MappingException("Falha ao mapear objeto(s)" + payloadJSONResult.getError().getMessage(), List.of());
            }

            Map<String, Object> payloadMap = payloadJSONResult.getValue();

            Instant exp = Instant.ofEpochSecond(((Number) payloadMap.get("exp")).longValue());

            if (Instant.now().isBefore(exp)) {
                return payloadMap.get("sub");
            }

            return false;
        }
    }
}
