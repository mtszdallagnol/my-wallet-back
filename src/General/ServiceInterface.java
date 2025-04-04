package General;

import Responses.ServiceResponse;

import java.sql.SQLException;
import java.util.List;

public interface ServiceInterface<T> {
    ServiceResponse<List<T>> getAll() throws Exception;

    ServiceResponse<T> getById(int id) throws Exception;

    ServiceResponse<Void> post(T data) throws Exception;

    ServiceResponse<Void> update(T data) throws Exception;

    ServiceResponse<Void> delete(int id) throws Exception;
}
