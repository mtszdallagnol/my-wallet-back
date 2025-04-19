package Wallets;

import Exceptions.InvalidParamsException;
import Exceptions.MappingException;
import General.GeneralController;
import Server.WebServer;
import Users.UserDTO;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class WalletController extends GeneralController {

    @Override
    protected void handleGET(Map<String, Object> params) {

        if (!user.getPerfil().equals(UserDTO.userType.ADMIN)) {
            params.put("id_usuario", user.getId());
        }

        WalletService walletService = new WalletService(conn);

        CompletableFuture<Object> responseFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return walletService.get(params);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, WebServer.dbThreadPool);

        responseFuture.thenAccept(result -> {
            response.error = false;
            response.msg = "Operação realizada com sucesso";
            response.data.put("data", result);
            response.httpStatus = 200;
            response.errors = null;

            try {
                WebServer.SendResponse(exchange, response);
            } catch (IOException e) {
                throw new RuntimeException("Erro ao enviar resposta", e);
            }
        });

        responseFuture.exceptionally(e -> {
            response.error = true;
            while (e.getCause() != null) {
                e = e.getCause();
            }

            response.msg = e.getMessage();
            response.data = null;

            if (e instanceof MappingException) {
                response.httpStatus = 400;
                response.errors = ((MappingException) e).getErrors();
            } else if (e instanceof InvalidParamsException) {
                response.httpStatus = 400;
                response.errors = ((InvalidParamsException) e).getErrors();
            } else {
                response.httpStatus = 500;
                response.errors = null;
            }

            try {
                WebServer.SendResponse(exchange, response);
            } catch (IOException ex) {
                throw new RuntimeException("Erro ao enviar resposta de erro", ex);
            }

            return null;
        });
    }

    @Override
    protected void handlePOST(Map<String, Object> params) {

        if (!user.getPerfil().equals(UserDTO.userType.ADMIN)) {
            params.put("id_usuario", user.getId());
        }

        WalletService walletService = new WalletService(conn);

        CompletableFuture<Object> responseFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return walletService.post(params);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, WebServer.dbThreadPool);

        responseFuture.thenAccept(result -> {
            response.error = false;
            response.msg = "Carteira criada com sucesso";
            response.data.put("data", result);
            response.httpStatus = 201;
            response.errors = null;

            try {
                WebServer.SendResponse(exchange, response);
            } catch (IOException e) {
                throw new RuntimeException("Erro ao enviar resposta", e);
            }
        });

        responseFuture.exceptionally(e -> {
            response.error = true;
            while (e.getCause() != null) {
                e = e.getCause();
            }

            response.msg = e.getMessage();
            response.data = null;

            if (e instanceof MappingException) {
                response.httpStatus = 400;
                response.errors = ((MappingException) e).getErrors();
            } else if (e instanceof InvalidParamsException) {
                response.httpStatus = 400;
                response.errors = ((InvalidParamsException) e).getErrors();
            } else {
                response.httpStatus = 500;
                response.errors = null;
            }

            try {
                WebServer.SendResponse(exchange, response);
            } catch (IOException ex) {
                throw new RuntimeException("Erro ao enviar resposta de erro", ex);
            }

            return null;
        });
    }

    @Override
    protected void handlePUT() {
    }

    @Override
    protected void handleDELETE(Map<String, Object> params) {
    }
}
