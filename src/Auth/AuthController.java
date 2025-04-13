package Auth;

import General.GeneralController;

import java.util.Map;

public class AuthController extends GeneralController {
    @Override
    protected void handleGET(Map<String, Object> params) {

        AuthService authService = new AuthService(conn);

        try {

        }
    }
}
