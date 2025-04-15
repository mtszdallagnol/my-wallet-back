package Auth;

import Exceptions.InvalidParamsException;
import Exceptions.MappingException;
import Exceptions.ValidationException;
import General.CryptoUtils;
import General.JsonParsers;
import Responses.ControllerResponse;
import Server.WebServer;
import Users.UserModel;
import Users.UserService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AuthController {
    ControllerResponse response = new ControllerResponse();
    private HttpExchange exchange;
    private Connection conn;

    public void handle(HttpExchange exchange, Connection conn) throws IOException, SQLException {
        this.exchange = exchange;
        this.conn = conn;

        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,Authorization");

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
                    exchange.sendResponseHeaders(500, -1);
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
        AuthService authService = new AuthService(conn);
        CompletableFuture.runAsync(() -> {
            try {
                List<UserModel> listResponse = userService.get(Map.of("email", login.get("email")));
                UserModel user = listResponse.isEmpty() ? null : listResponse.getFirst();

                if (user == null) {
                    response.error = true;
                    response.httpStatus = 400;
                    response.msg = "Email ou senha inválido(s)";
                    response.data = null;
                    response.errors = null;

                    WebServer.SendResponse(exchange, response);
                    return;
                }

                if (!user.getSenha().equals(
                        CryptoUtils.hashPassword(login.get("senha").toString(), user.getSalt()))) {
                    response.error = true;
                    response.httpStatus = 400;
                    response.msg = "Email pu senha inválido(s)";
                    response.data= null;
                    response.errors = null;

                    WebServer.SendResponse(exchange, response);
                    return;
                }

                AuthDTO.JwtToken accessTokenObject = AuthDTO.JwtToken.createJwtToken(AuthDTO.tokenType.ACCESS, user.getId());
                String accessToken = accessTokenObject.getHeader() + "." + accessTokenObject.getPayload() + "." + accessTokenObject.getSignature();
                AuthDTO.JwtToken refreshTokenObject = AuthDTO.JwtToken.createJwtToken(AuthDTO.tokenType.REFRESH, user.getId());
                String refreshToken = refreshTokenObject.getHeader() + "." + refreshTokenObject.getPayload() + "." + refreshTokenObject.getSignature();

                Map<String, Object> params = new HashMap<>();
                params.put("token", refreshToken);
                params.put("user_id", user.getId());
                authService.post(params);

                String cookie = String.format("refresh_token=%s; HttpOnly; Secure; Path=/; Max-Age=%d; SameSite=Strict",
                        refreshToken, 7 * 24 * 60 * 60);
                exchange.getResponseHeaders().add("Set-Cookie", cookie);

                response.error = false;
                response.httpStatus = 200;
                response.msg = "Logado com sucesso";
                response.data.put("access_token", accessToken);
                response.errors = null;

                WebServer.SendResponse(exchange, response);
            } catch (Exception e) {
                response.error = true;
                response.httpStatus = 500;
                response.msg = e.getMessage();
                response.data = null;
                response.errors = null;

                try { WebServer.SendResponse(exchange, response); } catch (IOException ex) { throw new RuntimeException(e); };
            }
        }, WebServer.dbThreadPool);
    }

    private void Logout() throws IOException {

    }

    public void SingUp(Map<String, Object> params) {
        CompletableFuture<Void> responseFuture;

        UserService userService = new UserService(conn);
        responseFuture = CompletableFuture.supplyAsync(() -> {
            try { userService.post(params); } catch (Exception e) { throw new RuntimeException(e); }
            return null;
        }, WebServer.dbThreadPool);

        responseFuture.exceptionally(e -> {
            response.error = true;
            while (e.getCause() != null) {
                e = e.getCause(); }
            response.msg = e.getMessage();

            switch (e) {
                case InvalidParamsException invalidParamsException -> {
                    response.httpStatus = 400;
                    response.errors = invalidParamsException.getErrors();
                }
                case MappingException mappingException -> {
                    response.httpStatus = 500;
                    response.errors = mappingException.getErrors();
                }
                case ValidationException validationException -> {
                    response.httpStatus = 400;
                    response.errors = validationException.getErrors();
                }
                default -> {
                    response.httpStatus = 500;
                    response.errors = null;
                }
            }

            try { WebServer.SendResponse(exchange, response); }
            catch (IOException ex) { throw new RuntimeException(ex); }

            return null;
        });

        responseFuture.thenRun(() -> {
            response.error = false;
            response.msg = "Sucesso ao cadastrar usuário";
            response.httpStatus = 200;
            response.data = null;
            response.errors = null;

            try { WebServer.SendResponse(exchange, response); }
            catch (IOException e) { throw new RuntimeException(e); }
        });
    }
}
