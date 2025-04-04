package General;

import Responses.ServiceResponse;

import java.sql.SQLException;
import java.util.List;

public interface ServiceInterface<T> {
    ServiceResponse<List<T>> getAll() throws Exception;

    ServiceResponse<T> getById(int id);

    ServiceResponse<Void> post(T data);

    ServiceResponse<Void> update(T data);

    ServiceResponse<Void> delete(int id);
}
