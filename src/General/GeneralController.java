package General;

import Responses.ControllerResponse;
import Server.WebServer;
import Users.UserModel;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

abstract public class GeneralController {
    protected ControllerResponse response = new ControllerResponse();
    protected HttpExchange exchange;
    protected Connection conn;
    protected UserModel user;

    public void handle(HttpExchange exchange) throws IOException, SQLException {
        // Implementar melhor jeito de lidar quando não tem nenhum conexão disponivel
        this.conn = WebServer.databaseConnectionPool.getConnection();
        this.exchange = exchange;

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
