package Users;

import Exceptions.InvalidParamsException;
import Exceptions.MappingException;
import General.GeneralController;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import Server.WebServer;

public class UserController extends GeneralController {

    @Override
    protected void handleGET(Map<String, Object> params) {

        if (user.getPerfil() != UserDTO.userType.ADMIN){
            response.error = true;
            response.httpStatus = 401;
            response.msg = "Operação Não autorizada";
            response.data = null;
            response.errors = null;

            try { WebServer.SendResponse(exchange,response); }
            catch (IOException e) { throw new RuntimeException(e); }
            return;
        }

        CompletableFuture<Object> responseFuture;

        UserService userService = new UserService(conn);
        responseFuture = CompletableFuture.supplyAsync(() -> {
            try { return userService.get(params); } catch (Exception e) { throw new RuntimeException(e); }
        });

        responseFuture.exceptionally(e -> {
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
        });

        responseFuture.thenAccept(result -> {
            response.error = false;
            response.msg = "Sucesso ao recuperar usuário(s)";
            response.httpStatus = 200;
            response.data.put("data", result);

            try { WebServer.SendResponse(exchange, response); }
            catch (Exception e) { throw new RuntimeException(e); }
        });
    }


    @Override
    protected void handlePOST(Map<String, Object> params) {

    }

    @Override
    protected void handlePUT() {

    }

    @Override
    protected void handleDELETE(Map<String, Object> params) {
        if (user.getPerfil() != UserDTO.userType.ADMIN) {
            response.error = true;
            response.httpStatus = 401;
            response.msg = "Operação Não autorizada";
            response.data = null;
            response.errors = null;

            try { WebServer.SendResponse(exchange, response); }
            catch (Exception e) { throw new RuntimeException(e); };

            return;
        }

        CompletableFuture<Void> responseFuture;

        UserService userService = new UserService(conn);
        responseFuture = CompletableFuture.supplyAsync(() -> {
            try { userService.delete(params); } catch (Exception e) { throw new RuntimeException(e); }
            return null;
        }, WebServer.dbThreadPool);

        responseFuture.exceptionally(e -> {
            response.error = true;
            while (e.getCause() != null) {
                e = e.getCause(); }
            response.msg = e.getMessage();

            if (e instanceof InvalidParamsException) {
                InvalidParamsException invalidParamsException = (InvalidParamsException) e;
                response.httpStatus = 400;
                response.errors = invalidParamsException.getErrors();
            } else {
                response.httpStatus = 500;
                response.errors = null;
            }


            try { WebServer.SendResponse(exchange, response); }
            catch (IOException ex) { throw new RuntimeException(ex); }

            return null;
        });

        responseFuture.thenRun(() -> {
            response.error = false;
            response.msg = "Sucesso ao deletar usuário";
            response.httpStatus = 200;
            response.data = null;
            response.errors = null;

            try { WebServer.SendResponse(exchange, response); }
            catch (IOException e) { throw new RuntimeException(e); }
        });
    }
}
