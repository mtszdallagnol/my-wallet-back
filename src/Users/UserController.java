package Users;

import General.GeneralController;
import Server.WebServer;
import General.Utils;

import java.util.concurrent.CompletableFuture;

import Responses.ControllerResponse;

public class UserController extends GeneralController {

    @Override
    protected void handleGET(Utils.queryType type, int targetId) {
        CompletableFuture<ControllerResponse<?>> responseFuture;

        UserService service = new UserService();
        if (type == Utils.queryType.MULTIPLE) {
            responseFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return service.getAll();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, WebServer.dbThreadPool);
        } else {
            responseFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return service.getById(targetId);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, WebServer.dbThreadPool);
        }

        responseFuture.thenAccept(result -> response = result);
    }


    @Override
    protected void handlePOST() {

    }

    @Override
    protected void handlePUT() {

    }

    @Override
    protected void handleDELETE(int id) {
        CompletableFuture<ControllerResponse<Void>> responseFuture;

        UserService service = new UserService();
        responseFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return service.delete(id);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        responseFuture.thenAccept(result -> response = result );
    }
}
