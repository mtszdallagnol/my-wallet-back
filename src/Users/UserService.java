package Users;

import General.ObjectMapper;
import General.ServiceInterface;
import Responses.ControllerResponse;
import Server.WebServer;

import java.sql.*;
import java.util.*;

public class UserService implements ServiceInterface<UserDTO> {
    @Override
    public ControllerResponse<List<UserDTO>> getAll() throws Exception {
        ControllerResponse<List<UserDTO>> response = new ControllerResponse<>();
        ObjectMapper<UserDTO> objectMapper = new ObjectMapper<>(UserDTO.class);

        Connection conn = WebServer.databaseConnectionPool.getConnection();
        PreparedStatement stmt = conn.prepareStatement(
                "SELECT * FROM usuarios"
        );
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

        List<String> errors = objectMapper.getErrors();
        if (!errors.isEmpty()) {
            response.error = true;
            response.httpStatus = 500;
            response.msg = "Erro ao mapear objeto do banco de dados";
            response.errors = errors;

            return response;
        }

        response.error = false;
        response.httpStatus = 200;
        response.msg = "Sucesso ao recuperar usuários";
        response.data = users;

        WebServer.databaseConnectionPool.returnConnection(conn);
        return response;
    }

    @Override
    public ControllerResponse<UserDTO> getById(int id) throws Exception {
        ControllerResponse<UserDTO> response = new ControllerResponse<>();
        ObjectMapper<UserDTO> objectMapper = new ObjectMapper<>(UserDTO.class);

        Connection conn = WebServer.databaseConnectionPool.getConnection();
        PreparedStatement stmt = conn.prepareStatement(
                "SELECT * FROM usuarios WHERE id = ?"
        );

        stmt.setInt(1, id);

        ResultSet rs = stmt.executeQuery();

        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        UserDTO user;
        if (rs.next()) {
            Map<String, Object> row = new HashMap<>();

            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                Object value = rs.getObject(columnName);
                row.put(columnName, value);
            }

            user = objectMapper.map(row);
        } else {
            response.error = true;
            response.httpStatus = 400;
            response.msg = "Nenhum registro com referente ID";

            return response;
        }

        List<String> errors = objectMapper.getErrors();
        if (!errors.isEmpty()) {
            response.error = true;
            response.httpStatus = 500;
            response.msg = "Erro ao mapear objeto do banco de dados";
            response.errors = errors;

            return response;
        }

        response.error = false;
        response.data = user;

        WebServer.databaseConnectionPool.returnConnection(conn);
        return response;
    }

    @Override
    public ControllerResponse<Void> post(UserDTO user) {
        return null;
    }

    @Override
    public ControllerResponse<Void> update(UserDTO user) {
        return null;
    }

    @Override
    public ControllerResponse<Void> delete (int id) throws Exception {
        ControllerResponse<Void> response = new ControllerResponse<>();

        Connection conn = WebServer.databaseConnectionPool.getConnection();
        PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM usuarios WHERE id=?"
        );

        stmt.setInt(1, id);

        int rowCount = stmt.executeUpdate();

        response.error = rowCount <= 0;
        response.httpStatus = response.error ? 400 : 200;
        response.msg = response.error ? "Nenhum registro com referente ID" : "Usuário removido com sucesso";
        response.rowChanges = rowCount;

        WebServer.databaseConnectionPool.returnConnection(conn);
        return response;
    }
}
