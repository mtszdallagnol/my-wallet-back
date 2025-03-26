package Users;

import Server.WebServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

import General.ResponseAPI;

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
                ResponseAPI response = new ResponseAPI();
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
        ResponseAPI response = new ResponseAPI();
        typeGet type = typeGet.INVALID;

        String query = exchange.getRequestURI().getQuery();
        if (query == null) {
            type = typeGet.MULTIPLE;
        } else {
            String[] params = query.split("&");
            if (params.length == 1 && params[0].contains("=")) {
                String[] keyValue = params[0].split("=");
                type = (keyValue.length == 2 && keyValue[0].equals("id")) ? typeGet.SINGLE : typeGet.INVALID;
            } else {
                type = typeGet.INVALID;
            }
        }

        if (type == typeGet.INVALID) {
            response.error = 1;
            response.httpStatus = 400;
            response.msg = "Tipo de parametro invalido";
            response.data = type;
            WebServer.SendResponse(exchange, response);
            return;
        }
//        Comecar a implementar paradas especificas com async - criar database conection pool >:(
//        CompletableFuture<List<UserDTO>> responseFuture= CompletableFuture.supplyAsync(() -> {
//
//        }, WebServer.dbThreadPool);
    }

    private static void handlePOST(HttpExchange exchange) throws IOException {
        ResponseAPI response = new ResponseAPI();


        WebServer.SendResponse(exchange, response);
    }

    private static void handlePUT(HttpExchange exchange) throws IOException {
        ResponseAPI response = new ResponseAPI();




        WebServer.SendResponse(exchange, response);
    }

    private static void handleDELETE(HttpExchange exchange) throws IOException {
        ResponseAPI response = new ResponseAPI();



        WebServer.SendResponse(exchange, response);
    }

}
