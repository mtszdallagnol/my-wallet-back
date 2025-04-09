package General;



import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class Utils {
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;

    public static Map<String, Object> getParamsFromQuery(String query) {
        Map<String, Object> params = new HashMap<>();

        if (query == null || query.trim().isEmpty()) {
            return params;
        }

        String[] listAllParams = query.split("&");
        for (String param : listAllParams) {
            if (param.isEmpty()) continue;

            String[] kv = param.split("=", 2);
            if (kv.length <= 1) continue;

            String value = kv[1];
            String key = kv[0];

            params.put(key, value);
        }

        return params;
    }

    public static String hashPassword(String password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        byte[] hash = factory.generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(hash);
    }

    public static byte[] getSalt() {
        SecureRandom sr = new SecureRandom();
        byte[] salt = new byte[16];

        sr.nextBytes(salt);

        return salt;
    }
}
