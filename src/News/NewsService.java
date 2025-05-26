package News;

import Exceptions.InvalidParamsException;
import Exceptions.MappingException;
import Exceptions.ValidationException;
import General.ObjectMapper;
import Interfaces.ServiceInterface;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class NewsService implements ServiceInterface<NewsModel> {

    private final Connection conn;

    public NewsService(Connection conn) {
        this.conn = conn;
    }

    @Override
    public List<NewsModel> get(Map<String, Object> params) throws InvalidParamsException, SQLException, MappingException {
        ObjectMapper<NewsModel> newsMapper = new ObjectMapper<>(NewsModel.class);

        List<String> invalidFields = params.keySet().stream()
                .filter(key -> !newsMapper.hasField(key))
                .collect(Collectors.toList());
        if (!invalidFields.isEmpty()) throw new InvalidParamsException(invalidFields);

        StringBuilder query = new StringBuilder("SELECT * FROM noticias");

        List<String> clauses = new ArrayList<>();
        List<Object> arguments = new ArrayList<>();

        for (String key : params.keySet()) {
            clauses.add(key + " = ?");
            arguments.add(params.get(key));
        }

        if (!clauses.isEmpty()) {
            query.append(" WHERE ").append(String.join(" AND ", clauses));
        }
        query.append(";");

        PreparedStatement stmt = conn.prepareStatement(query.toString());
        for (int i = 0; i < arguments.size(); i++) {
            stmt.setObject(i + 1, arguments.get(i));
        }

        ResultSet rs = stmt.executeQuery();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        List<NewsModel> response = new ArrayList<>();
        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                Object value = rs.getObject(columnName);
                row.put(columnName, value);
            }
            response.add(newsMapper.map(row));
        }

        List<String> errors = newsMapper.getErrors();
        if (!errors.isEmpty()) throw new MappingException(errors);

        return response;
    }

    @Override
    public NewsModel post(Map<String, Object> newsToPost) throws SQLException, InvalidParamsException, ValidationException, MappingException {
        ObjectMapper<NewsDTO.postRequirementModel> objectMapper = new ObjectMapper<>(NewsDTO.postRequirementModel.class);

        List<String> invalidFields = newsToPost.keySet().stream()
                .filter(key -> !objectMapper.hasField(key))
                .collect(Collectors.toList());
        if (!invalidFields.isEmpty()) throw new InvalidParamsException(invalidFields);

        List<String> validationErrors = objectMapper.executeValidation(newsToPost, conn);
        if (!validationErrors.isEmpty()) throw new ValidationException(validationErrors);

        List<String> errors = objectMapper.getErrors();
        if (!errors.isEmpty()) throw new MappingException(errors);

        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO noticias (titulo, categoria, conteudo, analista_id, status, data_criacao) VALUES (?, ?, ?, ?, COALESCE(?, 'ativo'), CURRENT_TIMESTAMP);",
                Statement.RETURN_GENERATED_KEYS);

        stmt.setString(1, (String) newsToPost.get("titulo"));
        stmt.setString(2, (String) newsToPost.get("categoria"));
        stmt.setString(3, (String) newsToPost.get("conteudo"));
        stmt.setInt(4, (Integer) newsToPost.get("analista_id"));
        stmt.setString(5, (String) newsToPost.getOrDefault("status", null));

        stmt.executeUpdate();

        ResultSet generatedKeys = stmt.getGeneratedKeys();
        generatedKeys.next();
        Object value = generatedKeys.getObject("GENERATED_KEY");

        return get(Map.of("id", value)).get(0);
    }

    @Override
    public NewsModel update(Map<String, Object> newsToUpdate) throws SQLException, InvalidParamsException, MappingException, ValidationException {
        List<String> updateFields = new ArrayList<>();
        List<Object> parameters = new ArrayList<>();

        for (Map.Entry<String, Object> column : newsToUpdate.entrySet()) {
            updateFields.add(column.getKey() + " = ?");
            parameters.add(column.getValue());
        }

        String query = "UPDATE noticias SET " + String.join(", ", updateFields) + " WHERE id = ?";

        parameters.add(newsToUpdate.get("id"));

        PreparedStatement stmt = conn.prepareStatement(query);
        for (int i = 0; i < parameters.size(); i++) {
            stmt.setObject(i + 1, parameters.get(i));
        }

        int count = stmt.executeUpdate();
        if (count < 1) throw new InvalidParamsException("Notícia não encontrada", List.of());

        return get(Map.of("id", newsToUpdate.get("id"))).get(0);
    }

    @Override
    public void delete(Map<String, Object> params) throws SQLException, InvalidParamsException {
        ObjectMapper<NewsModel> objectMapper = new ObjectMapper<>(NewsModel.class);

        if (!params.containsKey("id") || params.size() != 1) {
            System.out.println("ID recebido para deletar: " + params.get("id"));
            throw new InvalidParamsException("Parâmetro inválido para delete. Use apenas o 'id'.", List.of("id"));
        }

        List<String> invalidFields = params.keySet().stream()
                .filter(key -> !objectMapper.hasField(key))
                .collect(Collectors.toList());
        if (!invalidFields.isEmpty()) throw new InvalidParamsException(invalidFields);

        String query = "DELETE FROM noticias WHERE id = ?;";

        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setObject(1, params.get("id"));

        int count = stmt.executeUpdate();
        if (count < 1) throw new InvalidParamsException("Notícia não encontrada", List.of());
    }

}
