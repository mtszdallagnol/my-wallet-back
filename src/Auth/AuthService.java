package Auth;

import Exceptions.MappingException;
import General.ObjectMapper;
import Interfaces.ServiceInterface;

import java.sql.*;
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
            throw new MappingException(errors);
        }

        return response;
    }

    @Override
    public RefreshTokenModel post(Map<String, Object> params) throws MappingException, SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO refresh_tokens (token, expires_at, id_usuario)" +
                    " VALUES (?, ?, ?)",
        Statement.RETURN_GENERATED_KEYS);

        LocalDateTime now = LocalDateTime.now();
        stmt.setString(1, (String) params.get("token"));
        stmt.setTimestamp(2, Timestamp.valueOf(now.plusWeeks(1)));
        stmt.setInt(3, (int) params.get("id_usuario"));

        List<RefreshTokenModel> listResponse = get(Map.of("id_usuario", params.get("id_usuario")));
        if (!listResponse.isEmpty()) {
            delete(Map.of("id_usuario", params.get("id_usuario")));
        }

        stmt.executeUpdate();

        ResultSet generatedKeys = stmt.getGeneratedKeys();

        generatedKeys.next();
        Object value = generatedKeys.getObject("GENERATED_KEY");

        return get(Map.of("id", value)).get(0);
    }

    @Override
    public RefreshTokenModel update(Map<String, Object> params) throws SQLException, MappingException {
        return null;
    }

    @Override
    public void delete(Map<String, Object> params) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM refresh_tokens WHERE id_usuario = ?"
        );

        stmt.setInt(1, (int) params.get("id_usuario"));

        stmt.executeUpdate();
    }

    public AuthService(Connection conn) {
        this.conn = conn;
    }

    private final Connection conn;
}