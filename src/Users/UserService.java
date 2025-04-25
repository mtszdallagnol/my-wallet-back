package Users;

import Exceptions.InvalidParamsException;
import Exceptions.MappingException;
import Exceptions.ValidationException;
import General.CryptoUtils;
import General.ObjectMapper;
import Interfaces.ServiceInterface;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.*;
import java.util.*;

public class UserService implements ServiceInterface<UserModel> {

    private static final int SALT_SIZE = 16;

    @Override
    public List<UserModel> get(Map<String, Object> params) throws SQLException, MappingException, InvalidParamsException {
        ObjectMapper<UserModel> objectMapper = new ObjectMapper<>(UserModel.class);

        List<String> invalidFields = new ArrayList<>();
        for (String key : params.keySet()) {
            if (!objectMapper.hasField(key)) {
                invalidFields.add(key);
            }
        }
        if (!invalidFields.isEmpty()) throw new InvalidParamsException(invalidFields);

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

        List<UserModel> response = new ArrayList<>();
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
            throw new MappingException(errors);
        }

        return response;
    }

    @Override
    public UserModel post(Map<String, Object> userToPost) throws SQLException, NoSuchAlgorithmException, InvalidKeySpecException {
        ObjectMapper<UserDTO.postRequirementModel> objectMapper = new ObjectMapper<>(UserDTO.postRequirementModel.class);

        List<String> invalidFields = new ArrayList<>();
        for (String key : userToPost.keySet()) {
            if (!objectMapper.hasField(key)) {
                invalidFields.add(key);
            }
        }
        if (!invalidFields.isEmpty()) throw new InvalidParamsException(invalidFields);

        List<String> validationErrors = objectMapper.executeValidation(userToPost, conn);
        if (!validationErrors.isEmpty()) throw new ValidationException(validationErrors);

        List<String> errors = objectMapper.getErrors();
        if (!errors.isEmpty()) throw new MappingException(errors);

        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO usuarios (nome, email, senha, salt, perfil)" +
                    "VALUES (?, ?, ?, ?, ?);",
        Statement.RETURN_GENERATED_KEYS);

        byte[] salt = CryptoUtils.generateRandomSecureToken(SALT_SIZE);
        stmt.setString(1, (String) userToPost.get("nome"));
        stmt.setString(2, (String) userToPost.get("email"));
        stmt.setString(3, CryptoUtils.hashPassword((String) userToPost.get("senha"), salt));
        stmt.setBytes(4, salt);
        stmt.setString(5, userToPost.get("perfil").toString());

        stmt.executeUpdate();

        ResultSet generatedKeys = stmt.getGeneratedKeys();

        generatedKeys.next();
        Object value = generatedKeys.getObject("GENERATED_KEY");

        return get(Map.of("id", value)).get(0);
    }

    @Override
    public UserModel update(Map<String, Object> userToUpdate) throws SQLException {
        ObjectMapper<UserDTO.updateRequirementModel> objectMapper = new ObjectMapper<>(UserDTO.updateRequirementModel.class);

        List<String> invalidFields = new ArrayList<>();
        for (String key : userToUpdate.keySet()) {
            if (!objectMapper.hasField(key)) {
                invalidFields.add(key);
            }
        }
        if (!invalidFields.isEmpty()) throw new InvalidParamsException(invalidFields);

        List<String> validationErrors = objectMapper.executeValidation(userToUpdate, conn);
        if (!validationErrors.isEmpty()) throw new ValidationException(validationErrors);

        List<String> errors = objectMapper.getErrors();
        if (!errors.isEmpty()) throw new MappingException(errors);

        List<String> updateFields = new ArrayList<>();
        List<Object> parameters = new ArrayList<>();

        updateFields.add("nome = ?");
        parameters.add(userToUpdate.get("nome"));

        if (userToUpdate.containsKey("estilo_investidor")) {
            updateFields.add("estilo_investidor = ?");
            parameters.add(userToUpdate.get("estilo_investidor"));
        }

        String query = "UPDATE usuarios " +
                     "SET " + String.join(", ", updateFields) + " " +
                     "WHERE id = ?";

        parameters.add(userToUpdate.get("id"));

        PreparedStatement stmt = conn.prepareStatement(query);
        for (int i = 0; i < parameters.size(); i++) {
            stmt.setObject(i + 1, parameters.get(i).toString());
        }

        stmt.executeUpdate();

        return get(Map.of("id", userToUpdate.get("id"))).get(0);
    }

    @Override
    public void delete (Map<String, Object> params) throws SQLException, InvalidParamsException {
        ObjectMapper<UserModel> objectMapper = new ObjectMapper<>(UserModel.class);

        List<String> invalidFields = new ArrayList<>();
        for (String key : params.keySet()) {
            if (!objectMapper.hasField(key)) {
                invalidFields.add(key);
            }
        }
        if (!invalidFields.isEmpty()) throw new InvalidParamsException(invalidFields);

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

    public UserService(Connection conn) { this.conn = conn; }

    private final Connection conn;

}
