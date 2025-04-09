package Users;

import Exceptions.InvalidParamsException;
import Exceptions.MappingException;
import Exceptions.ValidationException;
import General.ObjectMapper;
import General.ServiceInterface;
import General.Utils;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.*;
import java.util.*;

public class UserService implements ServiceInterface<UserDTO> {
    @Override
    public List<UserDTO> get(Map<String, Object> params) throws SQLException, MappingException, InvalidParamsException {
        ObjectMapper<UserDTO> objectMapper = new ObjectMapper<>(UserDTO.class);

        List<String> invalidFields = new ArrayList<>();
        for (String key : params.keySet()) {
            if (!objectMapper.hasField(key)) {
                invalidFields.add(key);
            }
        }
        if (!invalidFields.isEmpty()) throw new InvalidParamsException("Parâmetro(s) inválido(s)", invalidFields);

        StringBuilder query = new StringBuilder("SELECT * FROM usuarios");
        if (!params.isEmpty()) {
            query.append(" WHERE ");

            for (String key : params.keySet()) {
                String temp = key + " = ? AND ";
                query.append(temp);
            }

            query.delete(query.length() - 5, query.length());
            query.append(";");
        }

        PreparedStatement stmt = conn.prepareStatement(query.toString());

        if (!params.isEmpty()) {
            int enumerator = 1;
            for (Object value : params.values()) {
                stmt.setObject(enumerator, value);
                enumerator++;
            }
        }

        ResultSet rs = stmt.executeQuery();

        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        List<UserDTO> response = new ArrayList<>();
        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();

            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                Object value = rs.getObject(columnName);
                row.put(columnName, value);
            }

             response.add(objectMapper.map(row));
        }

        List<String> errors = objectMapper.getErrors();
        if (!errors.isEmpty()) {
            throw new MappingException("Falha ao mapear objeto", errors);
        }

        return response;
    }

    @Override
    public void post(Map<String, Object> userToPost) throws SQLException, NoSuchAlgorithmException, InvalidKeySpecException {
        ObjectMapper<UserDTO> objectMapper = new ObjectMapper<>(UserDTO.class);

        List<String> invalidFields = new ArrayList<>();
        for (String key : userToPost.keySet()) {
            if (!objectMapper.hasField(key)) {
                invalidFields.add(key);
            }
        }
        if (!invalidFields.isEmpty()) throw new InvalidParamsException("Parâmetro(s) inválido(s)", invalidFields);
        UserDTO user = objectMapper.map(userToPost, conn);

        List<String> errors = objectMapper.getErrors();
        if (!errors.isEmpty()) {
            throw new MappingException("Falha ao mapear objeto", errors);
        }

        List<String> validationErrors = objectMapper.getValidationErrors();
        if (!validationErrors.isEmpty()) {
            throw new ValidationException("Falha de validação", validationErrors);
        }

        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO usuarios (nome, email, senha, salt, perfil)" +
                    "VALUES (?, ?, ?, ?, ?);"
        );

        byte[] salt = Utils.getSalt();
        stmt.setString(1, user.getNome());
        stmt.setString(2, user.getEmail());
        stmt.setString(3, Utils.hashPassword(user.getSenha(), salt));
        stmt.setBytes(4, salt);
        stmt.setString(5, user.getPerfil());

        stmt.executeUpdate();
    }

    @Override
    public void update(Map<String, Object> userToUpdate) {
        return;
    }

    @Override
    public void delete (Map<String, Object> params) throws SQLException, InvalidParamsException {
        ObjectMapper<UserDTO> objectMapper = new ObjectMapper<>(UserDTO.class);

        List<String> invalidFields = new ArrayList<>();
        for (String key : params.keySet()) {
            if (!objectMapper.hasField(key)) {
                invalidFields.add(key);
            }
        }
        if (!invalidFields.isEmpty()) throw new InvalidParamsException("Parâmetro(s) inválido(s)", invalidFields);

        StringBuilder query = new StringBuilder("DELETE FROM usuarios WHERE ");
        for (String key : params.keySet()) {
            String temp = key + " = ? AND ";
            query.append(temp);
        }
        query.delete(query.length() - 5, query.length());
        query.append(";");

        PreparedStatement stmt = conn.prepareStatement(query.toString());
        int enumerator = 1;
        for (Object value : params.values()) {
            stmt.setObject(enumerator, value);
            enumerator++;
        }

        stmt.executeUpdate();
    }

    public UserService(Connection conn) {
        this.conn = conn;
    }

    Connection conn;
}
