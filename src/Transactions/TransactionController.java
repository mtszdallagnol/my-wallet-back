package Transactions;

import Exceptions.InvalidParamsException;
import Exceptions.MappingException;
import Exceptions.ValidationException;
import General.GeneralController;
import Server.WebServer;
import Users.UserDTO;
import Wallets.WalletService;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class TransactionController extends GeneralController{

    @Override
    protected void handleGET(Map<String, Object> params){

        if(!user.getPerfil().equals(UserDTO.userType.ADMIN)){
            params.put("id_usuario", user.getId());
        }

        TransactionService transactionService = new TransactionService(conn);
        CompletableFuture.supplyAsync(() -> {
            try { return transactionService.get(params); }
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
    protected void handlePOST(Map<String, Object> params){
        if (!user.getPerfil().equals(UserDTO.userType.ADMIN)) {
            params.put("id_usuario", user.getId());
        }

        WalletService walletService = new WalletService(conn);
        CompletableFuture.supplyAsync(() -> {
            try { return walletService.post(params); }
            catch (Exception e) { throw new RuntimeException(e); }
        }, WebServer.dbThreadPool)
        .exceptionallyAsync(e -> {
            response.error = true;
            while (e.getCause() != null) {
                e = e.getCause(); }
            response.msg = e.getMessage();

            response.data = null;

            if (e instanceof MappingException) {
                response.httpStatus = 500;
                response.errors = ((MappingException) e).getErrors();
            } else if (e instanceof InvalidParamsException) {
                response.httpStatus = 400;
                response.errors = ((InvalidParamsException) e).getErrors();
            } else if (e instanceof ValidationException) {
                response.httpStatus = 400;
                response.errors = ((ValidationException) e).getErrors();
            } else {
                response.httpStatus = 500;
                response.errors = null;
            }

            try { WebServer.SendResponse(exchange, response); }
            catch (IOException ex) { throw new RuntimeException(ex); }

            return null;
        }, exchange.getHttpContext().getServer().getExecutor())
        .thenAcceptAsync(result -> {
            response.error = false;
            response.msg = "Sucesso ao criar transação";
            response.httpStatus = 201;
            response.data.put("data", result);

            try { WebServer.SendResponse(exchange, response); }
            catch (Exception e) { throw new RuntimeException(e); }
        }, exchange.getHttpContext().getServer().getExecutor());
    }
    @Override
    protected void handlePUT(Map<String, Object> params){
        if (!user.getPerfil().equals(UserDTO.userType.ADMIN) || !params.containsKey("id_usuario"))
            params.put("id_usuario", user.getId());

        WalletService walletService = new WalletService(conn);
        CompletableFuture.supplyAsync(() -> {
            try { return walletService.update(params); }
            catch (Exception e) { throw new RuntimeException(e); }
        }, WebServer.dbThreadPool)
        .exceptionallyAsync(e -> {
            response.error = true;
            while (e.getCause() != null) {
                e = e.getCause(); }
            response.msg = e.getMessage();

            if (e instanceof InvalidParamsException) {
                response.httpStatus = 400;
                response.errors = ((InvalidParamsException) e).getErrors();
            } else if (e instanceof MappingException) {
                response.httpStatus = 500;
                response.errors = ((MappingException) e).getErrors();
            } else if (e instanceof ValidationException) {
                response.httpStatus = 400;
                response.errors = ((ValidationException) e).getErrors();
            } else {
                response.httpStatus = 500;
                response.errors = null;
            }

            try { WebServer.SendResponse(exchange, response); }
            catch (IOException ex) { throw new RuntimeException(ex); }

            return null;
        }, exchange.getHttpContext().getServer().getExecutor())
        .thenAcceptAsync(updatedUser -> {
            response.error = false;
            response.httpStatus = 200;
            response.msg = "Sucesso ao atualizar transação";
            response.data.put("user", updatedUser);
            response.errors = null;

            try { WebServer.SendResponse(exchange, response); }
            catch (IOException e) { throw new RuntimeException(e); }
        }, exchange.getHttpContext().getServer().getExecutor());
    }

    @Override
    protected void handleDELETE(Map<String, Object> params){
        if (!user.getPerfil().equals(UserDTO.userType.ADMIN)) {
            params.put("id_usuario", user.getId());
        }

        WalletService walletService = new WalletService(conn);
        CompletableFuture.runAsync(() -> {
            try { walletService.delete(params); }
            catch (Exception e) { throw new RuntimeException(e); }
        }, WebServer.dbThreadPool)
        .exceptionallyAsync(e -> {
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

            try { WebServer.SendResponse(exchange, response); }
            catch (IOException ex) { throw new RuntimeException(ex); }

            return null;
        }, exchange.getHttpContext().getServer().getExecutor())
        .thenRunAsync(() -> {
            response.error = false;
            response.msg = "Sucesso ao deletar transação";
            response.httpStatus = 200;
            response.data = null;
            response.errors = null;

            try { WebServer.SendResponse(exchange, response); }
            catch (IOException e) { throw new RuntimeException(e); }
        }, exchange.getHttpContext().getServer().getExecutor());
    }
}
