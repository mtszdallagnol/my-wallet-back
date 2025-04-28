package Assets;

import Exceptions.InvalidParamsException;
import Exceptions.MappingException;
import General.GeneralController;
import Server.WebServer;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AssetsController extends GeneralController {
    @Override
    protected void handleGET(Map<String, Object> params) {
        AssetsService assetsService = new AssetsService(conn);
        CompletableFuture.supplyAsync(() -> {
            try { return assetsService.get(params); }
            catch (Exception e) { throw new RuntimeException(e); }
        }, WebServer.dbThreadPool)
        .exceptionallyAsync(e -> {
            response.error = true;
            while (e.getCause() != null) {
                e = e.getCause(); }
            response.msg = e.getMessage();
            response.data = null;
            if (e instanceof MappingException) {
                response.httpStatus = 400;
                response.errors = ((MappingException) e).getErrors();
            } else if (e instanceof InvalidParamsException) {
                response.httpStatus = 400;
                response.errors = ((InvalidParamsException) e).getErrors();
            }
            else {
                response.httpStatus = 500;
                response.errors = null;
            }

            try { WebServer.SendResponse(exchange, response); }
            catch (IOException ex) { throw new RuntimeException(ex); }

            return null;
        }, exchange.getHttpContext().getServer().getExecutor())
        .thenAcceptAsync(result -> {
            response.error = false;
            response.msg = "Sucesso ao recuperar transação(s)";
            response.httpStatus = 200;
            response.data.put("data", result);

            try { WebServer.SendResponse(exchange, response); }
            catch (Exception e) { throw new RuntimeException(e); }
        }, exchange.getHttpContext().getServer().getExecutor());
    }

    @Override
    protected void handlePOST(Map<String, Object> params) {

    }

    @Override
    protected void handlePUT(Map<String, Object> params) {

    }

    @Override
    protected void handleDELETE(Map<String, Object> params) {

    }
}
