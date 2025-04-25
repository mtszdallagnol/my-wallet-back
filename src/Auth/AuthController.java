package Auth;

import Exceptions.InvalidParamsException;
import Exceptions.MappingException;
import Exceptions.ValidationException;
import General.CryptoUtils;
import General.JsonParsers;
import Responses.ControllerResponse;
import Server.WebServer;
import Users.UserDTO;
import Users.UserModel;
import Users.UserService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class AuthController {
    ControllerResponse response = new ControllerResponse();
    private HttpExchange exchange;
    private Connection conn;

    public void handle(HttpExchange exchange, Connection conn) throws IOException, SQLException {
        this.exchange = exchange;
        this.conn = conn;

        String origin = exchange.getRequestHeaders().getFirst("Origin");
        if (origin != null) {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", origin);
            exchange.getResponseHeaders().add("Access-Control-Allow-Credentials", "true");
        } else exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization, Cookie");

        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);

            return;
        }

        String path = exchange.getRequestURI().getPath().replaceAll("/+$", "");
        path = path.substring(5);

        switch (exchange.getRequestMethod()) {
            case "POST": {
                String contentType = exchange.getRequestHeaders().getFirst("Content-Type");

                if (contentType == null || !contentType.contains("json")) {
                    response.error = true;
                    response.httpStatus = 400;
                    response.msg = "Payload não suportado";
                    response.data = null;
                    response.errors = null;

                    WebServer.SendResponse(exchange, response);
                    return;
                }

                JsonParsers.DeserializationResult<Map<String, Object>> result = JsonParsers.deserialize(exchange.getRequestBody());
                if (!result.isSuccess()) {
                    response.error = true;
                    response.httpStatus = 500;
                    response.msg = "Falha ao mapear JSON";
                    response.data = null;
                    response.errors = null;

                    WebServer.SendResponse(exchange, response);
                    return;
                }
                Map<String, Object> map = result.getValue();

                if (path.equalsIgnoreCase("/login")) {
                    Login(map);
                    break;
                } else if (path.equalsIgnoreCase("/signup")) {
                    SingUp(map);
                    break;
                }
            }
            case "DELETE": {
                if (path.equalsIgnoreCase("/logout")) {
                    Logout();
                    break;
                }
            }
            default: {
                response.error = true;
                response.httpStatus = 405;
                response.msg = "Operação não suportada";
                response.data = null;
                response.errors = null;

                WebServer.SendResponse(exchange, response);
                break;
            }
        }
    }

    private void Login(Map<String, Object> login) throws IOException {

        if (!login.containsKey("email") || !login.containsKey("senha")) {
            response.error = true;
            response.httpStatus = 400;
            response.msg = "Parâmetro(s) inválidos";
            response.data = null;
            response.errors = null;

            WebServer.SendResponse(exchange, response);
            return;
        }

        UserService userService = new UserService(conn);
        CompletableFuture.supplyAsync(() -> {
            try { List<UserModel> listResponse = userService.get(Map.of("email", login.get("email")));
                  return listResponse.isEmpty() ? null : listResponse.get(0); }
            catch (SQLException e) { throw new RuntimeException(e); }
        }, WebServer.dbThreadPool)
        .thenAcceptAsync(fetchedUser -> {
            if (fetchedUser == null) {
                response.error = true;
                response.httpStatus = 400;
                response.msg = "Email ou senha inválido(s)";
                response.data = null;
                response.errors = null;

                try { WebServer.SendResponse(exchange, response); }
                catch (IOException e) { throw new RuntimeException(e); }
                return;
            }

            try { if (!fetchedUser.getSenha().equals(
                        CryptoUtils.hashPassword(login.get("senha").toString(), fetchedUser.getSalt()))) {
                    response.error = true;
                    response.httpStatus = 400;
                    response.msg = "Email ou senha inválido(s)";
                    response.data= null;
                    response.errors = null;

                    WebServer.SendResponse(exchange, response);
                    return; } }
            catch (Exception e) { throw new RuntimeException(e); }

            try {
                // this should be executed with a db thread
                Tokens tokens = createTokens(fetchedUser.getId());

                String cookie = String.format("refresh_token=%s; HttpOnly; Path=/; Max-Age=%d; SameSite=Strict",
                        tokens.refreshToken, 7 * 24 * 60 * 60);
                exchange.getResponseHeaders().add("Set-Cookie", cookie);

                response.error = false;
                response.httpStatus = 200;
                response.msg = "Logado com sucesso";
                response.data.put("access_token", tokens.accessToken);
            } catch (Exception e) { throw new RuntimeException(e); }

            response.data.put("usuario", new UserDTO.returnedUser(fetchedUser));
            response.errors = null;

            try { WebServer.SendResponse(exchange, response); }
            catch (IOException e) { throw new RuntimeException(e); }
        }, exchange.getHttpContext().getServer().getExecutor());
    }

    private void Logout() throws IOException {

    }

    public void SingUp(Map<String, Object> params) {
        UserService userService = new UserService(conn);
        CompletableFuture.supplyAsync(() -> {
            try {  return userService.post(params); } catch (Exception e) { throw new RuntimeException(e); }
        }, WebServer.dbThreadPool)
        .exceptionallyAsync(e -> {
            response.error = true;
            while (e.getCause() != null) {
                e = e.getCause(); }
            response.msg = e.getMessage();

            if (e instanceof InvalidParamsException) {
                response.httpStatus = 400;
                response.errors = ((InvalidParamsException) e).getErrors();
            } else if (e instanceof MappingException) {
                response.httpStatus = 500;
                response.errors = ((MappingException) e).getErrors();
            } else if (e instanceof ValidationException) {
                response.httpStatus = 400;
                response.errors = ((ValidationException) e).getErrors();
            } else {
                response.httpStatus = 500;
                response.errors = null;
            }

            try { WebServer.SendResponse(exchange, response); }
            catch (IOException ex) { throw new RuntimeException(ex); }

            return null;
        }, exchange.getHttpContext().getServer().getExecutor())
        .thenAccept(result -> {
            Tokens tokens;
            try { tokens = createTokens(result.getId()); }
            catch (Exception e) { throw new RuntimeException(e); }

            String cookie = String.format("refresh_token=%s; HttpOnly; Path=/; Max-Age=%d; SameSite=Strict",
                    tokens.refreshToken, 7 * 24 * 60 * 60);
            exchange.getResponseHeaders().add("Set-Cookie", cookie);

            response.error = false;
            response.msg = "Sucesso ao cadastrar usuário";
            response.httpStatus = 200;
            response.data.put("access_token", tokens.accessToken);
            response.data.put("user", new UserDTO.returnedUser(result));
            response.errors = null;

            try { WebServer.SendResponse(exchange, response); }
            catch (IOException e) { throw new RuntimeException(e); }
        });
    }

    private static class Tokens {
        public String accessToken;
        public String refreshToken;

        public Tokens(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }
    }
    private Tokens createTokens(int userID) throws NoSuchAlgorithmException, InvalidKeyException, SQLException {
        AuthService authService = new AuthService(conn);

        AuthDTO.JwtToken accessTokenObject = AuthDTO.JwtToken.createJwtToken(AuthDTO.tokenType.ACCESS, userID);
        String accessToken = accessTokenObject.getHeader() + "." + accessTokenObject.getPayload() + "." + accessTokenObject.getSignature();
        AuthDTO.JwtToken refreshTokenObject = AuthDTO.JwtToken.createJwtToken(AuthDTO.tokenType.REFRESH, userID);
        String refreshToken = refreshTokenObject.getHeader() + "." + refreshTokenObject.getPayload() + "." + refreshTokenObject.getSignature();

        Map<String, Object> params = new HashMap<>();
        params.put("token", refreshToken);
        params.put("id_usuario", userID);
        authService.post(params);

        return new Tokens(accessToken, refreshToken);
    }
}
