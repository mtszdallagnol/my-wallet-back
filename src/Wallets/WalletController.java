package Wallets;

import Exceptions.InvalidParamsException;
import Exceptions.MappingException;
import Exceptions.ValidationException;
import General.GeneralController;
import General.ObjectMapper;
import Server.WebServer;
import Transactions.TransactionService;
import Users.UserDTO;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class WalletController extends GeneralController {

    @Override
    protected void handleGET(Map<String, Object> params) {

        if (!user.getPerfil().equals(UserDTO.userType.ADMIN)) {
            params.put("id_usuario", user.getId());
        }

        WalletService walletService = new WalletService(conn);
        TransactionService transactionService = new TransactionService(conn);

        CompletableFuture.supplyAsync(() -> {
            try {
                List<WalletModel> walletModels = walletService.get(params);
                List<WalletDTO.WalletWithTransacitions> result = new ArrayList<>();

                for (WalletModel currentWallet : walletModels) {
                    WalletDTO.WalletWithTransacitions temp = new WalletDTO.WalletWithTransacitions(currentWallet);

                    temp.transacoes = transactionService.get(Map.of("id_carteira", currentWallet.getId()));

                    result.add(temp);
                }

                return result;
            }
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
            response.msg = "Sucesso ao recuperar carteira(s)";
            response.httpStatus = 200;
            response.data.put("data", result);

            try { WebServer.SendResponse(exchange, response); }
            catch (Exception e) { throw new RuntimeException(e); }
        }, exchange.getHttpContext().getServer().getExecutor());
    }

    @Override
    protected void handlePOST(Map<String, Object> params) {

        if (!user.getPerfil().equals(UserDTO.userType.ADMIN) || !params.containsKey("id_usuario")) {
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
            response.msg = "Sucesso ao criar carteira";
            response.httpStatus = 201;
            response.data.put("data", result);

            try { WebServer.SendResponse(exchange, response); }
            catch (Exception e) { throw new RuntimeException(e); }
        }, exchange.getHttpContext().getServer().getExecutor());
    }

    @Override
    protected void handlePUT(Map<String, Object> params) {
        if (!user.getPerfil().equals(UserDTO.userType.ADMIN) || !params.containsKey("id_usuario"))
            params.put("id_usuario", user.getId());

        WalletService walletService = new WalletService(conn);
        CompletableFuture.supplyAsync(() -> {
            try {
                ObjectMapper<WalletDTO.updateRequirementModel> objectMapper = new ObjectMapper<>(WalletDTO.updateRequirementModel.class);

                if (params.size() < 2) throw new InvalidParamsException("Nenhum parâmetro enviado", List.of());

                List<String> invalidFields = new ArrayList<>();
                for (String key : params.keySet()) {
                    if (!objectMapper.hasField(key)) {
                        invalidFields.add(key);
                    }
                }
                if (!invalidFields.isEmpty()) throw new InvalidParamsException(invalidFields);

                if (params.size() < 2) throw new InvalidParamsException("Nenhum parâmetro enviado", List.of());

                List<String> validationErrors = objectMapper.executeValidation(params, conn);
                if (!validationErrors.isEmpty()) throw new ValidationException(validationErrors);

                List<String> errors = objectMapper.getErrors();
                if (!errors.isEmpty()) throw new MappingException(errors);

                return walletService.update(params);
            }
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
            response.msg = "Sucesso ao atualizar carteira";
            response.data.put("user", updatedUser);
            response.errors = null;

            try { WebServer.SendResponse(exchange, response); }
            catch (IOException e) { throw new RuntimeException(e); }
        }, exchange.getHttpContext().getServer().getExecutor());
    }
    @Override
    protected void handleDELETE(Map<String, Object> params) {

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
            response.msg = "Sucesso ao deletar carteira";
            response.httpStatus = 200;
            response.data = null;
            response.errors = null;

            try { WebServer.SendResponse(exchange, response); }
            catch (IOException e) { throw new RuntimeException(e); }
        }, exchange.getHttpContext().getServer().getExecutor());
    }
}
