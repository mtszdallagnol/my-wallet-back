package Users;

import Server.WebServer;
import General.Utils;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import Responses.ControllerResponse;
import Responses.ServiceResponse;

public class UserController {
    public static void handle(HttpExchange exchange) throws IOException {
        switch (exchange.getRequestMethod()) {
            case "GET":
                handleGET(exchange);
                break;
            case "POST":
                handlePOST(exchange);
                break;
            case "PUT":
                handlePUT(exchange);
                break;
            case "DELETE":
                handleDELETE(exchange);
                break;
            default:
                ControllerResponse<Void> response = new ControllerResponse<>();
                response.error = true;
                response.httpStatus = 405;
                response.msg = "Invalid Operation";
                WebServer.SendResponse(exchange, response);
        }
    }

    private static void handleGET(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        Utils.queryType type = Utils.getParamIdType(query);

        if (type == Utils.queryType.INVALID) {
            ControllerResponse<Void> response = new ControllerResponse<>();
            response.error = true;
            response.httpStatus = 400;
            response.msg = "Tipo de parametro invalido";
            WebServer.SendResponse(exchange, response);
            return;
        }

        UserService service = new UserService();
        if (type == Utils.queryType.SINGLE) {
            ControllerResponse<UserDTO> response = new ControllerResponse<>();

            int userId = Integer.parseInt(query.split("=")[1]);
            CompletableFuture<ServiceResponse<UserDTO>> responseFuture =
                    CompletableFuture.supplyAsync(() -> {
                        try {
                            return service.getById(userId);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }, WebServer.dbThreadPool);

            responseFuture.thenAccept(result -> {
                if (!result.isSuccessful) {
                    response.msg = "Falha ao recuperar usuário: " + result.msg;
                    if (result.msg.equals("Nenhum registro com referente ID")) {
                            response.httpStatus = 400;
                    }
                } else {
                    response.error = false;
                    response.httpStatus = 200;
                    response.msg = result.msg;
                    response.data = result.data;
                }

                try {
                    WebServer.SendResponse(exchange, response);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } else {
            ControllerResponse<List<UserDTO>> response = new ControllerResponse<>();

            CompletableFuture<ServiceResponse<List<UserDTO>>> responseFuture =
                    CompletableFuture.supplyAsync(() -> {
                        try {
                            return service.getAll();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }, WebServer.dbThreadPool);

            responseFuture.thenAccept(result -> {
                if (!result.isSuccessful) {
                    response.msg = "Falha ao recuperar usuários: " + result.msg;
                } else {
                    response.error = false;
                    response.httpStatus = 200;
                    response.msg = "Sucesso ao recuperar usuários";
                    response.data = result.data;
                }

                try {
                    WebServer.SendResponse(exchange, response);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private static void handlePOST(HttpExchange exchange) throws IOException {
        ControllerResponse response = new ControllerResponse();


        WebServer.SendResponse(exchange, response);
    }

    private static void handlePUT(HttpExchange exchange) throws IOException {
        ControllerResponse response = new ControllerResponse();




        WebServer.SendResponse(exchange, response);
    }

    private static void handleDELETE(HttpExchange exchange) throws IOException {
        ControllerResponse<ServiceResponse<Void>> response = new ControllerResponse<>();

        String query = exchange.getRequestURI().getQuery();
        Utils.queryType type = Utils.getParamIdType(query);

        if (type == Utils.queryType.INVALID || type == Utils.queryType.MULTIPLE) {
            response.error = true;
            response.httpStatus = 400;
            response.msg = "Tipo de parametro invalido";
            WebServer.SendResponse(exchange, response);
            return;
        }

        UserService service = new UserService();
        CompletableFuture<ServiceResponse<Void>> responseFuture = CompletableFuture.supplyAsync(() -> {
            try {
                int userId = Integer.parseInt(query.split("=")[1]);
                return service.delete(userId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        responseFuture.thenAccept(result -> {
            if (!result.isSuccessful) {
                response.error = true;
                response.httpStatus = 400;
                response.msg = "Falha ao deletar usuário: " + result.msg;
            } else {
                response.error = false;
                response.httpStatus = 200;
                response.msg = result.msg;
            }

            try {
                WebServer.SendResponse(exchange, response);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }


}
