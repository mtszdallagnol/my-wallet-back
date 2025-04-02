package Users;

import Server.WebServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

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
                ControllerResponse response = new ControllerResponse();
                response.error = 1;
                response.httpStatus = 405;
                response.msg = "Invalid Operation";
                WebServer.SendResponse(exchange, response);
        }
    }

    private enum typeGet{
        SINGLE, MULTIPLE, INVALID
    }
    private static void handleGET(HttpExchange exchange) throws IOException {
        ControllerResponse response = new ControllerResponse();

        String query = exchange.getRequestURI().getQuery();
        typeGet type = getTypeGet(query);

        if (type == typeGet.INVALID) {
            response.error = 1;
            response.httpStatus = 400;
            response.msg = "Tipo de parametro invalido";
            response.data = type;
            WebServer.SendResponse(exchange, response);
            return;
        }

        try {
            CompletableFuture<ServiceResponse> responseFuture;

            UserService service = new UserService();
            if (type == typeGet.SINGLE) {
                int userId = Integer.parseInt(query.split("=")[1]);
                responseFuture = CompletableFuture.supplyAsync(() -> service.getById(userId), WebServer.dbThreadPool);
            } else {
                responseFuture = CompletableFuture.supplyAsync(service::getAll, WebServer.dbThreadPool);
            }

            responseFuture.thenAccept(result -> {
                if (!result.isSuccessful) {
                    throw new CompletionException(new RuntimeException("Falha ao recuperar usuário(s)"));
                }

                response.error = 0;
                response.httpStatus = 200;
                response.msg = "Sucesso ao recuperar usuário(s): " + result.msg;
                response.data = result.data;
            }).exceptionally(ex -> {
                response.error = 0;
                response.httpStatus = 500;
                response.msg = ex.getMessage();
                response.data = response.data;
                return null;
            }).whenComplete((result, ex) -> {
                
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
