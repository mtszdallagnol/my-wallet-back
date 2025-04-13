package Auth;

import Anotations.MaxLength;
import Anotations.Required;

public class AuthDTO {

    public static class Tokens {
        public final String accessToken;
        public final String refreshToken;

        public Tokens(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }
    }

    public class Login {
        @Required
        @MaxLength(254)
        public String email;
        @Required
        @MaxLength(255)
        public String password;

        public Login(String email, String password) {
            this.email = password;
            this.password = password;
        }
    }
}
