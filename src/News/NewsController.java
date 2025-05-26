package News;

import Exceptions.InvalidParamsException;
import Exceptions.MappingException;
import Exceptions.ValidationException;
import General.GeneralController;
import Server.WebServer;
import Users.UserDTO;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class NewsController extends GeneralController {
    private Executor getExecutor() {
        return exchange.getHttpContext().getServer().getExecutor();
    }

    private void handleException(Throwable e, int defaultStatusCode) {
        response.error = true;
        response.data = null;

        while (e.getCause() != null) e = e.getCause();

        response.msg = e.getMessage();

        if (e instanceof MappingException) {
            response.httpStatus = 400;
            response.errors = ((MappingException) e).getErrors();
        } else if (e instanceof InvalidParamsException) {
            response.httpStatus = 400;
            response.errors = ((InvalidParamsException) e).getErrors();
        } else if (e instanceof ValidationException) {
            response.httpStatus = 400;
            response.errors = ((ValidationException) e).getErrors();
        } else {
            response.httpStatus = defaultStatusCode;
            response.errors = null;
        }

        try {
            WebServer.SendResponse(exchange, response);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    protected void handleGET(Map<String, Object> params) {
        if (user.getPerfil() == UserDTO.userType.ANALISTA) {
            params.put("analista_id", user.getId());
        }

        NewsService newsService = new NewsService(conn);
        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return newsService.get(params);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }, WebServer.dbThreadPool)
                .exceptionallyAsync(e -> {
                    handleException(e, 500);
                    return null;
                }, getExecutor())
                .thenAcceptAsync(result -> {
                    response.error = false;
                    response.msg = "Sucesso ao recuperar notícia(s)";
                    response.httpStatus = 200;
                    response.data.put("data", result);

                    try {
                        WebServer.SendResponse(exchange, response);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, getExecutor());
    }

    @Override
    protected void handlePOST(Map<String, Object> params) {
        if (!(user.getPerfil() == UserDTO.userType.ANALISTA || user.getPerfil() == UserDTO.userType.ADMIN)) {
            response.error = true;
            response.msg = "Apenas analistas e admins podem criar notícias";
            response.httpStatus = 403;
            response.data = null;
            response.errors = null;

            try {
                WebServer.SendResponse(exchange, response);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return;
        }

        params.put("analista_id", user.getId());
        NewsService newsService = new NewsService(conn);

        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return newsService.post(params);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }, WebServer.dbThreadPool)
                .exceptionallyAsync(e -> {
                    handleException(e, 500);
                    return null;
                }, getExecutor())
                .thenAcceptAsync(result -> {
                    response.error = false;
                    response.msg = "Sucesso ao criar notícia";
                    response.httpStatus = 201;
                    response.data.put("data", result);

                    try {
                        WebServer.SendResponse(exchange, response);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, getExecutor());
    }

    @Override
    protected void handlePUT(Map<String, Object> params) {
        if (!(user.getPerfil() == UserDTO.userType.ANALISTA || user.getPerfil() == UserDTO.userType.ADMIN)) {
            response.error = true;
            response.msg = "Apenas analistas e admins podem editar notícias";
            response.httpStatus = 403;
            response.data = null;
            response.errors = null;

            try {
                WebServer.SendResponse(exchange, response);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return;
        }

        params.put("analista_id", user.getId());
        NewsService newsService = new NewsService(conn);

        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return newsService.update(params);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }, WebServer.dbThreadPool)
                .exceptionallyAsync(e -> {
                    handleException(e, 500);
                    return null;
                }, getExecutor())
                .thenAcceptAsync(result -> {
                    response.error = false;
                    response.msg = "Sucesso ao atualizar notícia";
                    response.httpStatus = 200;
                    response.data.put("data", result);
                    response.errors = null;

                    try {
                        WebServer.SendResponse(exchange, response);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, getExecutor());
    }

    @Override
    protected void handleDELETE(Map<String, Object> params) {
        if (!(user.getPerfil() == UserDTO.userType.ANALISTA || user.getPerfil() == UserDTO.userType.ADMIN)) {
            response.error = true;
            response.httpStatus = 401;
            response.msg = "Operação Não autorizada";
            response.data = null;
            response.errors = null;

            try { WebServer.SendResponse(exchange, response); }
            catch (Exception e) { throw new RuntimeException(e); };

            return;
        }


        NewsService newsService = new NewsService(conn);
        CompletableFuture.runAsync(() -> {
                    try { newsService.delete(params); } catch (Exception e) { throw new RuntimeException(e); }
                }, WebServer.dbThreadPool)
                .exceptionallyAsync(e -> {
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
                }, exchange.getHttpContext().getServer().getExecutor())
                .thenRunAsync(() -> {
                    response.error = false;
                    response.msg = "Sucesso ao deletar nóticia";
                    response.httpStatus = 200;
                    response.data = null;
                    response.errors = null;

                    try { WebServer.SendResponse(exchange, response); }
                    catch (IOException e) { throw new RuntimeException(e); }
                }, getExecutor());
    }
}
