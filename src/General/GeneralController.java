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
import java.util.concurrent.CompletableFuture;

abstract public class GeneralController {
    protected ControllerResponse response = new ControllerResponse();
    protected HttpExchange exchange;
    protected Connection conn;
    protected UserModel user;

    public void handle(HttpExchange exchange, Connection conn) throws IOException, NoSuchAlgorithmException, InvalidKeyException {
        this.exchange = exchange;
        this.conn = conn;

        setCorsPolicy();
        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) { return; }

        verifyAuthHeaders(exchange.getRequestHeaders())
        .thenAcceptAsync(authSuccess -> {
            if (authSuccess) { try { processRequest(); }
                             catch (IOException e) { throw new RuntimeException(e); } }
        }, exchange.getHttpContext().getServer().getExecutor());
    }

    private CompletableFuture<Boolean> verifyAuthHeaders(Headers headers) throws IOException, NoSuchAlgorithmException, InvalidKeyException {
        if (!headers.containsKey("Authorization")) {
            response.error = true;
            response.httpStatus = 401;
            response.msg = "Access Token faltando";
            response.data = null;
            response.errors = null;

            WebServer.SendResponse(exchange, response);
            return CompletableFuture.completedFuture(false);
        }

        String accessToken = headers.getFirst("Authorization");
        if (!accessToken.contains("Bearer")) {
            response.error = true;
            response.httpStatus = 401;
            response.msg = "Modelo de autenticação não suportado";
            response.data = null;
            response.errors = null;

            WebServer.SendResponse(exchange, response);
            return CompletableFuture.completedFuture(false);
        }
        accessToken = accessToken.substring(7);

        AuthDTO.JwtTokenValidationResponse accessTokenValidation = AuthDTO.JwtToken.verifyJwtToken(accessToken);
        UserService userService = new UserService(conn);

        if (accessTokenValidation.getValidationResponseType().equals(AuthDTO.tokenVerificationResponseType.VALID)) {
            return CompletableFuture.supplyAsync(() -> {
                int userID = accessTokenValidation.getUserID();
                try { user = userService.get(Map.of("id", userID)).get(0); }
                catch (SQLException e) { throw new RuntimeException(e); }

                return true;
            }, WebServer.dbThreadPool);
        } else if (accessTokenValidation.getValidationResponseType().equals(AuthDTO.tokenVerificationResponseType.INVALID)) {
            response.error = true;
            response.httpStatus = 401;
            response.msg = "Access Token inválido";
            response.data = null;
            response.errors = null;

            WebServer.SendResponse(exchange, response);
            return CompletableFuture.completedFuture(false);
        }

        if (!headers.containsKey("Cookie")) {
            response.error = true;
            response.httpStatus = 401;
            response.msg = "Access Token expirado e cookies faltando";
            response.data = null;
            response.errors = null;

            WebServer.SendResponse(exchange, response);
            return CompletableFuture.completedFuture(false);
        }

        String refresh_token = extractRefreshTokenFromCookies(headers);
        if (refresh_token == null)  {
            response.error = true;
            response.httpStatus = 401;
            response.msg = "Access Token expirado e Refresh Token faltando";
            response.data = null;
            response.errors = null;

            WebServer.SendResponse(exchange, response);
            return CompletableFuture.completedFuture(false);
        }

        AuthService authService = new AuthService(conn);
        return CompletableFuture.supplyAsync(() -> {
            try { List<RefreshTokenModel> resultList = authService.get(Map.of("token", refresh_token));
                  return resultList.isEmpty() ? null : resultList.get(0); }
            catch (SQLException e) { throw new RuntimeException(e); }
        }, WebServer.dbThreadPool)
        .thenComposeAsync(refreshTokenDatabase -> {
            if (refreshTokenDatabase == null) {
                response.error = true;
                response.httpStatus = 401;
                response.msg = "Access Token expirado e refresh token inválido";
                response.data = null;
                response.errors = null;

                try { WebServer.SendResponse(exchange, response); }
                catch (IOException e) { throw new RuntimeException(e); };
                return CompletableFuture.completedFuture(false);
            }

            if (!Instant.now().isBefore(Instant.ofEpochMilli(refreshTokenDatabase.getExpires_at().getTime()))) {
                CompletableFuture.runAsync(() -> {
                    response.error = true;
                    response.httpStatus = 401;
                    response.msg = "Access Token expirado, por favor logue novamente";
                    response.data = null;
                    response.errors = null;

                    Map<String, Object> params = Map.of("token", refreshTokenDatabase.getToken(),
                            "user_id", refreshTokenDatabase.getUser_id());
                    try { authService.delete(params); }
                    catch (SQLException e) { throw new RuntimeException(e); }
                }, WebServer.dbThreadPool);

                try { WebServer.SendResponse(exchange, response); }
                catch (IOException e) { throw new RuntimeException(e); }
                return CompletableFuture.completedFuture(false);
            }

            try {
                AuthDTO.JwtToken accessTokenObject = AuthDTO.JwtToken.createJwtToken(
                        AuthDTO.tokenType.ACCESS,
                        refreshTokenDatabase.getUser_id());

                String accessTokenString = accessTokenObject.getHeader() + "." + accessTokenObject.getPayload() + "." + accessTokenObject.getSignature();
                response.data.put("access_token", accessTokenString);

                return CompletableFuture.supplyAsync(() -> {
                    try {  user = userService.get(Map.of("id", refreshTokenDatabase.getUser_id())).get(0);
                           return true; }
                    catch (SQLException e) { throw new RuntimeException(e); }
                }, WebServer.dbThreadPool);
            } catch (Exception e) { throw new RuntimeException(e); }
        }, exchange.getHttpContext().getServer().getExecutor());
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

    private void setCorsPolicy() throws IOException {
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        if (origin != null) {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", origin);
            exchange.getResponseHeaders().add("Access-Control-Allow-Credentials", "true");
        } else exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization, Cookie");

        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
        }
    }

    private void processRequest() throws IOException {
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

                try { WebServer.SendResponse(exchange, response); }
                catch (IOException e) { throw new RuntimeException(e); }
                return;
            }

            JsonParsers.DeserializationResult<Map<String, Object>> result = JsonParsers.deserialize(exchange.getRequestBody());

            if (!result.isSuccess()) {
                response.error = true;
                response.httpStatus = 500;
                response.msg = "Erro ao deserializar JSON: " + result.getError().getMessage();
                response.data = null;
                response.errors = null;

                try { WebServer.SendResponse(exchange, response); }
                catch (IOException e) { throw new RuntimeException(e); }
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

    private String extractRefreshTokenFromCookies(Headers headers) {
        List<String> cookies = headers.get("Cookie");
        for (String cookieHeader : cookies) {
            String[] individualCookies = cookieHeader.split(";");
            for (String cookie : individualCookies) {
                String[] parts = cookie.trim().split("=", 2);
                if (parts.length == 2 && parts[0].equals("refresh_token")) {
                    return parts[1];
                }
            }
        }
        return null;
    }

    abstract protected void handleGET(Map<String, Object> params);

    abstract protected void handlePOST(Map<String, Object> params);

    abstract protected void handlePUT();

    abstract protected void handleDELETE(Map<String, Object> params);
}
