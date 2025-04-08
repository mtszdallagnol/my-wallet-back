package General;

import Responses.ControllerResponse;
import Users.UserDTO;

import java.util.List;

public interface ServiceInterface<T> {
    ControllerResponse<List<UserDTO>> getAll() throws Exception;

    ControllerResponse<UserDTO> getById(int id) throws Exception;

    ControllerResponse<Void> post(T data) throws Exception;

    ControllerResponse<Void> update(T data) throws Exception;

    ControllerResponse<Void> delete(int id) throws Exception;
}
