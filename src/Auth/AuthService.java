package Auth;

import Exceptions.MappingException;
import General.CryptoUtils;
import General.JsonParsers;
import General.ObjectMapper;
import Interfaces.ServiceInterface;
import Server.WebServer;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

public class AuthService implements ServiceInterface<RefreshTokenModel> {

    @Override
    public List<RefreshTokenModel> get(Map<String, Object> params) throws SQLException, MappingException {
        ObjectMapper<RefreshTokenModel> objectMapper = new ObjectMapper<>(RefreshTokenModel.class);

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

        List<RefreshTokenModel> response = new ArrayList<>();
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
            throw new MappingException("Erro ao mapear objeto(s)", errors);
        }

        return response;
    }

    @Override
    public Optional<RefreshTokenModel> post(Map<String, Object> params) throws MappingException, SQLException {
        ObjectMapper<RefreshTokenModel> objectMapper = new ObjectMapper<>(RefreshTokenModel.class);

        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO refresh_tokens (token, expires_at, user_id)" +
                    " VALUES (?, ?, ?)",
        Statement.RETURN_GENERATED_KEYS);

        LocalDateTime now = LocalDateTime.now();
        stmt.setString(1, (String) params.get("token"));
        stmt.setTimestamp(2, Timestamp.valueOf(now.plusWeeks(1)));
        stmt.setInt(3, (int) params.get("user_id"));

        List<RefreshTokenModel> listResponse = get(Map.of("user_id", params.get("user_id")));
        if (!listResponse.isEmpty()) {
            delete(Map.of("user_id", params.get("user_id")));
        }

        stmt.executeUpdate();

        ResultSet generatedKeys = stmt.getGeneratedKeys();
        ResultSetMetaData metaData = generatedKeys.getMetaData();
        int columnCount = metaData.getColumnCount();

        RefreshTokenModel response = null;
        if (generatedKeys.next()) {
            Map<String, Object> row = new HashMap<>();

            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                Object value = generatedKeys.getObject(columnName);
                row.put(columnName, value);
            }

            response = objectMapper.map(row);
        }

        List<String> errors = objectMapper.getErrors();
        if (!errors.isEmpty()) {
            throw new MappingException("Falha ao mapear objeto(s)", errors);
        }

        return Optional.ofNullable(response);
    }

    @Override
    public Optional<RefreshTokenModel> update(Map<String, Object> params) throws SQLException, MappingException {
        ObjectMapper<RefreshTokenModel> objectMapper = new ObjectMapper<>(RefreshTokenModel.class);

        PreparedStatement stmt = conn.prepareStatement("UPDATE refresh_tokens" +
                " SET token = ?, expires_at = ?, created_at = ?" +
                " WHERE user_id = ?;",
        Statement.RETURN_GENERATED_KEYS);

        LocalDateTime now = LocalDateTime.now();
        stmt.setString(1, (String) params.get("token"));
        stmt.setTimestamp(2, Timestamp.valueOf(now.plusWeeks(1)));
        stmt.setTimestamp(3, Timestamp.valueOf(now));
        stmt.setInt(4, (int) params.get("user_id"));

        stmt.executeUpdate();

        ResultSet rs = stmt.getGeneratedKeys();

        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        RefreshTokenModel response = null;
        if (rs.next()) {
            Map<String, Object> row = new HashMap<>();

            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                Object value = rs.getObject(columnName);
                row.put(columnName, value);
            }

            response = objectMapper.map(row);
        }

        List<String> errors = objectMapper.getErrors();
        if (!errors.isEmpty()) {
            throw new MappingException("Falha ao mapear objeto(s)", errors);
        }

        return Optional.ofNullable(response);
    }

    @Override
    public void delete(Map<String, Object> params) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM refresh_tokens WHERE user_id = ?"
        );

        stmt.setInt(1, (int) params.get("user_id"));

        stmt.executeUpdate();
    }

    public static Object verifyJwtToken(String token) throws NoSuchAlgorithmException, InvalidKeyException, MappingException {
        String[] parts = token.split("\\.");

        if (parts.length != 3) {
            return false;
        }

        String headerEncoded = parts[0];
        String payloadEncoded = parts[1];
        String providedSignature = parts[2];

        String dataToSign = headerEncoded + "." + payloadEncoded;
        String expectedSignature = CryptoUtils.generateSHA256Signature(dataToSign, WebServer.JWT_SECRET_KEY);

        if (!expectedSignature.equals(providedSignature)) {
            return false;
        }

        String payload = new String(Base64.getDecoder().decode(payloadEncoded), StandardCharsets.UTF_8);
        JsonParsers.DeserializationResult<Map<String, Object>> payloadJSONResult = JsonParsers.deserialize(payload);

        if (!payloadJSONResult.isSuccess()) {
            throw new MappingException("Falha ao mapear objeto(s)" + payloadJSONResult.getError().getMessage(), List.of());
        }

        Map<String, Object> payloadMap = payloadJSONResult.getValue();

        Instant exp = Instant.ofEpochSecond(((Number) payloadMap.get("exp")).longValue());

        if (Instant.now().isBefore(exp)) {
            return Integer.parseInt((String) payloadMap.get("sub"));
        }

        return false;
    }

    public AuthService(Connection conn) {
        this.conn = conn;
    }

    private final Connection conn;
}