package Users;

import java.util.ArrayList;

interface UserInterface {
    ArrayList<UserDTO> getAll();

    UserDTO getById(int id);

    void post(UserDTO user);

    void update(UserDTO user);

    void delete(int id);

}
