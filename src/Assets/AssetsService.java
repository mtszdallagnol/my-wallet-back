package Assets;

import Exceptions.InvalidParamsException;
import Exceptions.MappingException;
import General.ObjectMapper;
import Interfaces.ServiceInterface;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssetsService implements ServiceInterface<AssetsModel> {
    @Override
    public List<AssetsModel> get(Map<String, Object> params) throws Exception {
        ObjectMapper<AssetsModel> objectMapper = new ObjectMapper<>(AssetsModel.class);

        List<String> invalidFields = new ArrayList<>();
        for (String key : params.keySet()) {
            if (!objectMapper.hasField(key)) {
                invalidFields.add(key);
            }
        }
        if (!invalidFields.isEmpty()) throw new InvalidParamsException(invalidFields);

        StringBuilder query = new StringBuilder("SELECT * FROM ativos");

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

        List<AssetsModel> response = new ArrayList<>();
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
    public AssetsModel post(Map<String, Object> userToPost) throws Exception {
        return null;
    }

    @Override
    public AssetsModel update(Map<String, Object> params) throws Exception {
        return null;
    }

    @Override
    public void delete(Map<String, Object> params) throws Exception {

    }

    public AssetsService(Connection conn) { this.conn = conn; }

    private Connection conn;
}
