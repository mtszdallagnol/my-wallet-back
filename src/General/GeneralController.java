package General;

import Responses.ControllerResponse;
import Server.WebServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

abstract public class GeneralController {
    protected ControllerResponse<?> response = new ControllerResponse<>();

    public void handle(HttpExchange exchange) throws IOException {
        String requestType = exchange.getRequestMethod();

        if (requestType.equalsIgnoreCase("GET")) {
            String query = exchange.getRequestURI().getQuery();
            Utils.queryType type = Utils.getParamIdType(query);

            if (type == Utils.queryType.INVALID) {
                response.error = true;
                response.httpStatus = 400;
                response.msg = "Parametrização da requesição inválida";
            }

            handleGET(type, type == Utils.queryType.SINGLE ?
                    Integer.parseInt(query.split("=")[1]) : 0);

            WebServer.SendResponse(exchange, response);
        } else if (requestType.equalsIgnoreCase("POST")) {
            System.out.println("POST");
        } else if (requestType.equalsIgnoreCase("PUT")) {
            System.out.println("PUT");
        } else if (requestType.equalsIgnoreCase("DELETE")) {
            String query = exchange.getRequestURI().getQuery();
            Utils.queryType type = Utils.getParamIdType(query);

            if (type == Utils.queryType.INVALID || type == Utils.queryType.MULTIPLE) {
                response.error = true;
                response.httpStatus = 400;
                response.msg = "Parametrização da requesição inválida";
            }

            handleDELETE(Integer.parseInt(query.split("=")[1]));

            WebServer.SendResponse(exchange, response);
        } else {
            response.error = true;
            response.httpStatus = 405;
            response.msg = "Método http não suportado";

            WebServer.SendResponse(exchange, response);
        }
    }

    abstract protected void handleGET(Utils.queryType type, int id);

    abstract protected void handlePOST();

    abstract protected void handlePUT();

    abstract protected void handleDELETE(int id);
}
