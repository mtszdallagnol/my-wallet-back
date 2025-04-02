package Users;

import General.ServiceInterface;
import Responses.ServiceResponse;

import java.util.List;

public class UserService implements ServiceInterface<UserDTO> {
    @Override
    public ServiceResponse<List<UserDTO>> getAll() {
        return null;
    }

    @Override
    public ServiceResponse<UserDTO> getById(int id) {
        return null;
    }

    @Override
    public ServiceResponse post(UserDTO user) {
        return null;
    }

    @Override
    public ServiceResponse update(UserDTO user) {
        return null;
    }

    @Override
    public ServiceResponse delete (int id) {
        return null;
    }
}
