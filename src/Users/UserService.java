package Users;

import Exceptions.InvalidParamsException;
import Exceptions.MappingException;
import Exceptions.ValidationException;
import General.ObjectMapper;
import General.ServiceInterface;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.*;
import java.util.*;

public class UserService implements ServiceInterface {
    @Override
    public List<UserModel> get(Map<String, Object> params) throws SQLException, MappingException, InvalidParamsException {
        ObjectMapper<UserModel> objectMapper = new ObjectMapper<>(UserModel.class);

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
            throw new MappingException("Falha ao mapear objeto", errors);
        }

        return response;
    }

    @Override
    public Optional<Object> post(Map<String, Object> userToPost) throws SQLException, NoSuchAlgorithmException, InvalidKeySpecException {
        ObjectMapper<UserModel> objectMapper = new ObjectMapper<>(UserModel.class);

        List<String> invalidFields = new ArrayList<>();
        for (String key : userToPost.keySet()) {
            if (!objectMapper.hasField(key)) {
                invalidFields.add(key);
            }
        }
        if (!invalidFields.isEmpty()) throw new InvalidParamsException("Parâmetro(s) inválido(s)", invalidFields);
        UserModel user = objectMapper.map(userToPost, conn);

        List<String> errors = objectMapper.getErrors();
        if (!errors.isEmpty()) {
            throw new MappingException("Falha ao mapear objeto", errors);
        }

        List<String> validationErrors = objectMapper.getValidationErrors();
        if (!validationErrors.isEmpty()) {
            throw new ValidationException("Falha de validação", validationErrors);
        }

        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO usuarios (nome, email, senha, salt)" +
                    "VALUES (?, ?, ?, ?);"
        );

        byte[] salt = getSalt();
        stmt.setString(1, user.getNome());
        stmt.setString(2, user.getEmail());
        stmt.setString(3, hashPassword(user.getSenha(), salt));
        stmt.setBytes(4, salt);

        stmt.executeUpdate();

        return Optional.empty();
    }

    @Override
    public Optional<Object> update(Map<String, Object> userToUpdate) {
        return null;
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

    public static String hashPassword(String password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        byte[] hash = factory.generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(hash);
    }

    private static byte[] getSalt() {
        SecureRandom sr = new SecureRandom();
        byte[] salt = new byte[16];

        sr.nextBytes(salt);

        return salt;
    }

    public UserService(Connection conn) {
        this.conn = conn;
    }

    private final Connection conn;

    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;
}
