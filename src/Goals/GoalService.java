package Goals;

import Exceptions.InvalidParamsException;
import Exceptions.MappingException;
import Exceptions.ValidationException;
import General.ObjectMapper;
import Interfaces.ServiceInterface;
import Transactions.TransactionModel;
import Users.UserModel;
import Wallets.WalletModel;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class GoalService implements ServiceInterface<GoalModel> {

    public List<GoalModel> get(Map<String, Object> params) throws Exception {
        ObjectMapper<GoalModel> gMapper = new ObjectMapper<>(GoalModel.class);
        ObjectMapper<WalletModel> wMapper = new ObjectMapper<>(WalletModel.class);
        ObjectMapper<UserModel> uMapper = new ObjectMapper<>(UserModel.class);

        List<String> invalidFields = params.keySet().stream()
                .filter(key -> !gMapper.hasField(key)
                        && !wMapper.hasField(key)
                        && !uMapper.hasField(key))
                .collect(Collectors.toList());
        if (!invalidFields.isEmpty()) throw new InvalidParamsException(invalidFields);

        List<String> tKeys = new ArrayList<>();
        List<String> wKeys = new ArrayList<>();
        List<String> uKeys = new ArrayList<>();

        for (String key : params.keySet()) {
            if      (gMapper.hasField(key)) tKeys.add(key);
            else if (wMapper.hasField(key)) wKeys.add(key);
            else                            uKeys.add(key);
        }

        StringBuilder query = new StringBuilder(
                "SELECT m.* FROM metas m"
        );

        if (!wKeys.isEmpty() || !uKeys.isEmpty()) query.append(" INNER JOIN carteiras c ON c.id = m.id_carteira");
        if (!uKeys.isEmpty()) query.append(" INNER JOIN usuarios u ON u.id = c.id_usuario");

        List<String> clauses   = new ArrayList<>();
        List<Object> arguments = new ArrayList<>();

        for (String key : tKeys) {
            arguments.add(params.get(key));

            key = key.contains(".") ? key.substring(key.indexOf(".") + 1) : key;
            clauses.add("m." + key + " = ?");
        }

        for (String key : wKeys) {
            arguments.add(params.get(key));

            key = key.contains(".") ? key.substring(key.indexOf(".") + 1) : key;
            clauses.add("c." + key + " = ?");
        }

        for (String key : uKeys) {
            arguments.add(params.get(key));

            key = key.contains(".") ? key.substring(key.indexOf(".") + 1) : key;
            clauses.add("u." + key + " = ?");
        }

        if (!clauses.isEmpty()) {
            query.append(" WHERE ")
                    .append(String.join(" AND ", clauses))
                    .append(";");
        } else query.append(";");

        PreparedStatement stmt = conn.prepareStatement(query.toString());

        for (int i = 0; i < arguments.size(); i++) {
            stmt.setObject(i + 1, arguments.get(i));
        }
        ResultSet rs = stmt.executeQuery();

        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        List<GoalModel> response = new ArrayList<>();
        while(rs.next()) {
            Map<String, Object> row = new HashMap<>();

            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                Object value = rs.getObject(i);

                row.put(columnName, value);
            }

            response.add(gMapper.map(row));
        }

        List<String> errors = gMapper.getErrors();
        if (!errors.isEmpty()) throw new MappingException(errors);

        return response;
    }

    public GoalModel post(Map<String, Object> goalToPost) throws Exception {
        ObjectMapper<GoalDTO.postRequirementModel> objectMapper = new ObjectMapper<>(GoalDTO.postRequirementModel.class);

        List<String> invalidKeys = new ArrayList<>();
        for (String key : goalToPost.keySet()) {
            if (!objectMapper.hasField(key)) {
                invalidKeys.add(key);
            }
        }
        if (!invalidKeys.isEmpty()) throw new InvalidParamsException(invalidKeys);

        List<String> validationErrors = objectMapper.executeValidation(goalToPost, conn);
        if (!validationErrors.isEmpty()) throw new ValidationException(validationErrors);

        List<String> errors = objectMapper.getErrors();
        if (!errors.isEmpty()) throw new MappingException(errors);

        goalToPost.remove("id_usuario");

        goalToPost.put("data_inicial", Date.valueOf(LocalDate.now()));

        String columns = String.join(", ", goalToPost.keySet());
        String placeholder = String.join(", ", Collections.nCopies(goalToPost.size(), "?"));

        String query = "INSERT INTO metas (" + columns + ") VALUES (" + placeholder + ")";

        PreparedStatement stmt = conn.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS);

        int enumerator = 1;
        for (Object value : goalToPost.values()) {
            if (value == null) stmt.setNull(enumerator++, Types.NULL);
            else stmt.setString(enumerator++, value.toString());
        }

        stmt.executeUpdate();

        ResultSet generatedKeys = stmt.getGeneratedKeys();

        generatedKeys.next();

        Object id = generatedKeys.getObject(1);
        return get(Map.of("id", id)).get(0);
    }

    public GoalModel update(Map<String, Object> params) throws Exception {
        return null;
    }

    public void delete(Map<String, Object> params) throws Exception {
        ObjectMapper<GoalModel> gMapper = new ObjectMapper<>(GoalModel.class);
        ObjectMapper<WalletModel> wMapper = new ObjectMapper<>(WalletModel.class);
        ObjectMapper<UserModel> uMapper = new ObjectMapper<>(UserModel.class);

        List<String> invalidFields = params.keySet().stream()
                .filter(key -> !gMapper.hasField(key)
                        && !wMapper.hasField(key)
                        && !uMapper.hasField(key))
                .collect(Collectors.toList());
        if (!invalidFields.isEmpty()) throw new InvalidParamsException(invalidFields);

        List<String> tKeys = new ArrayList<>();
        List<String> wKeys = new ArrayList<>();
        List<String> uKeys = new ArrayList<>();

        for (String key : params.keySet()) {
            if      (gMapper.hasField(key)) tKeys.add(key);
            else if (wMapper.hasField(key)) wKeys.add(key);
            else                            uKeys.add(key);
        }

        StringBuilder query = new StringBuilder(
                "DELETE m FROM metas AS m"
        );

        if (!wKeys.isEmpty() || !uKeys.isEmpty()) query.append(" INNER JOIN carteiras c ON c.id = m.id_carteira");
        if (!uKeys.isEmpty()) query.append(" INNER JOIN usuarios u ON u.id = c.id_usuario");

        List<String> clauses   = new ArrayList<>();
        List<Object> arguments = new ArrayList<>();

        for (String key : tKeys) {
            arguments.add(params.get(key));

            key = key.contains(".") ? key.substring(key.indexOf(".") + 1) : key;
            clauses.add("m." + key + " = ?");
        }

        for (String key : wKeys) {
            arguments.add(params.get(key));

            key = key.contains(".") ? key.substring(key.indexOf(".") + 1) : key;
            clauses.add("c." + key + " = ?");
        }

        for (String key : uKeys) {
            arguments.add(params.get(key));

            key = key.contains(".") ? key.substring(key.indexOf(".") + 1) : key;
            clauses.add("u." + key + " = ?");
        }

        if (!clauses.isEmpty()) {
            query.append(" WHERE ")
                    .append(String.join(" AND ", clauses))
                    .append(";");
        } else query.append(";");

        PreparedStatement stmt = conn.prepareStatement(query.toString());

        for (int i = 0; i < arguments.size(); i++) {
            stmt.setObject(i + 1, arguments.get(i));
        }
        int count = stmt.executeUpdate();
        if (count < 1) throw new InvalidParamsException("Meta(s) não encontrada(s)", List.of());
    }

    public GoalService(Connection conn) { this.conn = conn; }

    private final Connection conn;
}
