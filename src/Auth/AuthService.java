package Auth;

import Exceptions.InvalidParamsException;
import Exceptions.MappingException;
import Exceptions.ValidationException;
import General.JWTUtil;
import General.ObjectMapper;
import General.ServiceInterface;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.*;
import java.util.*;

public class AuthService implements ServiceInterface {
    @Override
    public List<RefreshTokenModel> get(Map<String, Object> params) throws SQLException {
        ObjectMapper<RefreshTokenModel> objectMapper = new ObjectMapper<>(RefreshTokenModel.class);

        List<String> invalidFields = new ArrayList<>();
        for (String key : params.keySet()) {
            if (!objectMapper.hasField(key)) {
                invalidFields.add(key);
            }
        }
        if (!invalidFields.isEmpty()) throw new InvalidParamsException("Parâmetro(s) inválido(s)", invalidFields);

        StringBuilder query = new StringBuilder("SELECT * FROM refresh_tokens");
        if (!params.isEmpty()) {
            query.append(" WHERE ");

            for (String key : params.keySet()) {
                String temp = key + " = ? AND ";
                query.append(temp);
            }

            query.delete(query.length() - 5, query.length());
            query.append(";");
        }

        PreparedStatement stmt = conn.prepareStatement(query);

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

        List<RefreshTokenModel> response = new ArrayList<>();
        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();

            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(1);
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
    public Optional<Object> post(Map<String, Object> params) throws SQLException, NoSuchAlgorithmException, InvalidKeyException, InvalidParamsException {
        List<String> invalidFields = new ArrayList<>();
        for (String key : params.keySet()) {
            if (!key.equals("id_usuario")) {
                invalidFields.add(key);
            }
        }
        if (!invalidFields.isEmpty()) throw new InvalidParamsException("Parâmetro(s) inválido(s)", invalidFields);

        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO refresh_tokens (token, id_usuario, expires_at)" +
                    " VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, JWTUtil.generateRefreshToken(params.get("id_usuario").toString()));
        stmt.setInt(2, (int) params.get("id_usuario"));
        stmt.setTimestamp(3, new Timestamp((System.currentTimeMillis() / 1000) + JWTUtil.REFRESH_TOKEN_EXPIRATION_TIME));

        stmt.executeUpdate();

        ResultSet rs = stmt.getGeneratedKeys();
        if (rs.next()) {
            return Optional.of(rs.getString("token"));
        }

        return Optional.empty();
    }

    public AuthService(Connection conn) { this.conn = conn; }

    private final Connection conn;
}