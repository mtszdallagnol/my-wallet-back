package Users;

import General.ObjectMapper;
import General.ServiceInterface;
import Responses.ServiceResponse;
import Server.WebServer;

import java.sql.*;
import java.util.*;

public class UserService implements ServiceInterface<UserDTO> {
    @Override
    public ServiceResponse<List<UserDTO>> getAll() throws Exception {
        ServiceResponse<List<UserDTO>> response = new ServiceResponse<>();

        Connection conn = WebServer.databaseConnectionPool.getConnection();
        ObjectMapper<UserDTO> objectMapper = new ObjectMapper<>(UserDTO.class);

        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM usuarios");
        ResultSet rs = stmt.executeQuery();

        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        List<UserDTO> users = new ArrayList<>();
        while(rs.next()) {
            Map<String, Object> row = new HashMap<>();

            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                Object value = rs.getObject(i);
                row.put(columnName, value);
            }

            users.add(objectMapper.map(row));
        }

        List<String> errors = objectMapper.getErros();
        if (!errors.isEmpty()) {
            response.isSuccessful = false;
            response.msg = "Erro ao mapear objeto do banco de dados: " + String.join(", ", errors);

            return response;
        }

        response.isSuccessful = true;
        response.data = users;

        return response;
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
