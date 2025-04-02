package General;

import Responses.ServiceResponse;
import java.util.concurrent.CompletableFuture;

public interface ServiceInterface<T> {
    ServiceResponse getAll();

    ServiceResponse getById(int id);

    ServiceResponse post(T data);

    ServiceResponse update(T data);

    ServiceResponse delete(int id);
}
