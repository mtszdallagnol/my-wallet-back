package General;

import Auth.AuthDTO;
import Auth.AuthService;
import Auth.RefreshTokenModel;
import Responses.ControllerResponse;
import Server.WebServer;
import Users.UserModel;
import Users.UserService;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

abstract public class GeneralController {
    protected ControllerResponse response = new ControllerResponse();
    protected HttpExchange exchange;
    protected Connection conn;
    protected UserModel user;

    public void handle(HttpExchange exchange, Connection conn) throws IOException, SQLException, NoSuchAlgorithmException, InvalidKeyException {
        this.exchange = exchange;
        this.conn = conn;

        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,Authorization");

        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);

            return;
        }

        verifyAuthHeaders(exchange.getRequestHeaders());

        String requestType = exchange.getRequestMethod();
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");

        Map<String, Object> params = getParamsFromQuery(exchange.getRequestURI().getQuery());

        if (requestType.equalsIgnoreCase("GET")) {

            handleGET(params);

        } else if (requestType.equalsIgnoreCase("POST")) {

            if (contentType == null || !contentType.contains("json")) {
                response.error = true;
                response.httpStatus = 400;
                response.msg = "Tipo de payload não suportado atualmente";
                response.data = null;
                response.errors = null;

                WebServer.SendResponse(exchange, response);
                return;
            }

            JsonParsers.DeserializationResult<Map<String, Object>> result = JsonParsers.deserialize(exchange.getRequestBody());

            if (!result.isSuccess()) {
                response.error = true;
                response.httpStatus = 500;
                response.msg = "Erro ao deserializar JSON: " + result.getError().getMessage();
                response.data = null;
                response.errors = null;

                WebServer.SendResponse(exchange, response);
                return;
            }

            Map<String, Object> dataMap = result.getValue();

            handlePOST(dataMap);

        } else if (requestType.equalsIgnoreCase("PUT")) {

            System.out.println("PUT");

        } else if (requestType.equalsIgnoreCase("DELETE")) {

            handleDELETE(params);

        } else {
            response.error = true;
            response.httpStatus = 405;
            response.msg = "Método http não suportado";

            WebServer.SendResponse(exchange, response);
        }
    }

    private void verifyAuthHeaders(Headers headers) throws IOException,
            NoSuchAlgorithmException, InvalidKeyException, SQLException {
        if (!headers.containsKey("Authorization")) {
            response.error = true;
            response.httpStatus = 401;
            response.msg = "Access Token faltando";
            response.data = null;
            response.errors = null;

            WebServer.SendResponse(exchange, response);
            return;
        }

        String accessToken = headers.getFirst("Authorization");
        if (!accessToken.contains("Bearer")) {
            response.error = true;
            response.httpStatus = 401;
            response.msg = "Modelo de autenticação não suportado";
            response.data = null;
            response.errors = null;

            WebServer.SendResponse(exchange, response);
            return;
        }
        accessToken = accessToken.substring(7);

        Object accessTokenValidation = AuthService.verifyJwtToken(accessToken);
        UserService userService = new UserService(conn);
        if (!Boolean.FALSE.equals(accessTokenValidation)) {
            int userID = (Integer) accessTokenValidation;
            user = userService.get(Map.of("id", userID)).get(0);
            return;
        }

        if (!headers.containsKey("Cookie")) {
            response.error = true;
            response.httpStatus = 401;
            response.msg = "Access Token inválido cookies faltando";
            response.data = null;
            response.errors = null;

            WebServer.SendResponse(exchange, response);
            return;
        }

        String refresh_token = null;
        List<String> cookies = headers.get("Cookie");
        for (String cookieHeader : cookies) {
            String[] individualCookies = cookieHeader.split(";");
            for (String cookie : individualCookies) {
                String[] parts = cookie.trim().split("=", 2);
                if (parts.length == 2 && parts[0].equals("refresh_token")) refresh_token = parts[1];
            }
        }
        if (refresh_token == null)  {
            response.error = true;
            response.httpStatus = 401;
            response.msg = "Access Token inválido e Refresh Token faltando";
            response.data = null;
            response.errors = null;

            WebServer.SendResponse(exchange, response);
            return;
        }

        AuthService authService = new AuthService(conn);
        List<RefreshTokenModel> resultList = authService.get(Map.of("token", refresh_token));
        RefreshTokenModel refreshTokenDatabase = resultList.isEmpty() ? null : resultList.get(0);
        if (refreshTokenDatabase == null) {
            response.error = true;
            response.httpStatus = 401;
            response.msg = "Access Token and Refresh Token inválidos";
            response.data = null;
            response.errors = null;

            WebServer.SendResponse(exchange, response);
            return;
        }

        if (!Instant.now().isBefore(Instant.ofEpochMilli(refreshTokenDatabase.getExpires_at().getTime()))) {
            response.error = true;
            response.httpStatus = 401;
            response.msg = "Access Token expirado, por favor logue novamente";
            response.data = null;
            response.errors = null;

            Map<String, Object> params = Map.of("token", refreshTokenDatabase.getToken(),
                    "user_id", refreshTokenDatabase.getUser_id());
            authService.delete(params);

            WebServer.SendResponse(exchange, response);
            return;
        }

        AuthDTO.JwtToken accessTokenObject = AuthDTO.JwtToken.createJwtToken(AuthDTO.tokenType.ACCESS,
                refreshTokenDatabase.getUser_id());
        String accessTokenString = accessTokenObject.getHeader() + "." + accessTokenObject.getPayload() + "." + accessTokenObject.getSignature();
        response.data.put("access_token", accessTokenString);
    }

    private static Map<String, Object> getParamsFromQuery(String query) {
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

    abstract protected void handleGET(Map<String, Object> params);

    abstract protected void handlePOST(Map<String, Object> params);

    abstract protected void handlePUT();

    abstract protected void handleDELETE(Map<String, Object> params);
}
