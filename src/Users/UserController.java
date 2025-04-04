package Users;

import Server.WebServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.sql.SQLException;
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

    private enum typeGet{
        SINGLE, MULTIPLE, INVALID
    }
    private static void handleGET(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        typeGet type = getTypeGet(query);

        if (type == typeGet.INVALID) {
            ControllerResponse<Void> response = new ControllerResponse<>();
            response.error = true;
            response.httpStatus = 400;
            response.msg = "Tipo de parametro invalido";
            WebServer.SendResponse(exchange, response);
            return;
        }

        UserService service = new UserService();
        if (type == typeGet.SINGLE) {
            ControllerResponse<UserDTO> response = new ControllerResponse<>();

            int userId = Integer.parseInt(query.split("=")[1]);
            CompletableFuture<ServiceResponse<UserDTO>> responseFuture =
                    CompletableFuture.supplyAsync(() -> service.getById(userId), WebServer.dbThreadPool);

            responseFuture.thenAccept(result -> {
                if (!result.isSuccessful) {
                    response.msg = "Falha ao recuperar usuário: " + result.msg;
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
        ControllerResponse response = new ControllerResponse();



        WebServer.SendResponse(exchange, response);
    }

    private static typeGet getTypeGet(String query) {
        if (query == null) {
            return typeGet.MULTIPLE;
        } else {
            String[] params = query.split("&");
            if (params.length == 1 && params[0].contains("=")) {
                String[] keyValue = params[0].split("=");
                return (keyValue.length == 2 && keyValue[0].equals("id")) ? typeGet.SINGLE : typeGet.INVALID;
            } else {
                return typeGet.INVALID;
            }
        }
    }
}
