package Wallets;

import Exceptions.MappingException;
import Exceptions.ValidationException;
import General.ObjectMapper;
import Interfaces.ServiceInterface;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

import Exceptions.InvalidParamsException;
import Users.UserModel;


public class WalletService implements ServiceInterface<WalletModel> {

    @Override
    public List<WalletModel> get(Map<String, Object> params) throws InvalidParamsException, SQLException {
        ObjectMapper<WalletModel> wMapper = new ObjectMapper<>(WalletModel.class);
        ObjectMapper<UserModel> uMapper = new ObjectMapper<>(UserModel.class);

        List<String> invalidFields = params.keySet().stream()
                .filter(key -> !wMapper.hasField(key)
                        && !uMapper.hasField(key))
                .collect(Collectors.toList());
        if(!invalidFields.isEmpty()) throw new InvalidParamsException(invalidFields);


        List<String> wKeys = new ArrayList<>();
        List<String> uKeys = new ArrayList<>();

        for (String key : params.keySet()) {
            if (wMapper.hasField(key)) wKeys.add(key);
            else                            uKeys.add(key);
        }

        StringBuilder query = new StringBuilder("SELECT c.* FROM carteiras c");

        if (!uKeys.isEmpty()) query.append(" INNER JOIN usuarios u ON u.id = c.id_usuario");

        List<String> clauses   = new ArrayList<>();
        List<Object> arguments = new ArrayList<>();

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

        if(!clauses.isEmpty()){
            query.append(" WHERE ")
                    .append(String.join(" AND ", clauses))
                    .append(";");
        }else query.append(";");

        PreparedStatement stmt = conn.prepareStatement(query.toString());

        for (int i = 0; i < arguments.size(); i++){
            stmt.setObject(i + 1, arguments.get(i));
        }
        ResultSet rs = stmt.executeQuery();

        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        List<WalletModel> response = new ArrayList<>();
        while (rs.next()){
            Map<String, Object> row = new HashMap<>();

            for(int i = 1; i <= columnCount; i++){
                String columnName = metaData.getColumnName(i);
                Object value = rs.getObject(columnName);
                row.put(columnName, value);
            }

            response.add(wMapper.map(row));
        }

        List<String> errors = wMapper.getErrors();
        if(!errors.isEmpty()){
            throw new MappingException(errors);
        }

        return response;
    }

    @Override
    public WalletModel post(Map<String, Object> walletToPost) throws SQLException, InvalidParamsException, ValidationException, MappingException {
        ObjectMapper<WalletDTO.postRequirementModel> objectMapper = new ObjectMapper<>(WalletDTO.postRequirementModel.class);

        List<String> invalidFields = new ArrayList<>();
        for (String key : walletToPost.keySet()){
            if(!objectMapper.hasField(key) && !key.equals("id_usuario")){
                invalidFields.add(key);
            }
        }
        if(!invalidFields.isEmpty()) throw new InvalidParamsException(invalidFields);

        List<String> validationErrors = objectMapper.executeValidation(walletToPost, conn);
        if (!validationErrors.isEmpty()) throw new ValidationException(validationErrors);

        List<String> errors = objectMapper.getErrors();
        if (!errors.isEmpty()) throw new MappingException(errors);

        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO carteiras (nome, descricao, id_usuario)" +
                        "VALUES (?, ?, ?);",
                Statement.RETURN_GENERATED_KEYS);

        stmt.setString(1, (String) walletToPost.get("nome"));
        stmt.setString(2, (String) walletToPost.get("descricao"));
        stmt.setInt(3, (Integer) walletToPost.get("id_usuario"));

        stmt.executeUpdate();

        ResultSet generatedKeys = stmt.getGeneratedKeys();

        generatedKeys.next();
        Object value = generatedKeys.getObject("GENERATED_KEY");

        return get(Map.of("id", value)).get(0);
    }

    @Override
    public WalletModel update(Map<String, Object> walletToUpdate) throws SQLException, InvalidParamsException, MappingException, ValidationException {

        List<String> updateFields = new ArrayList<>();
        List<Object> parameters = new ArrayList<>();

        for (Map.Entry<String, Object> column : walletToUpdate.entrySet()) {
            updateFields.add(column.getKey() + " = ?");
            parameters.add(column.getValue());
        }

        String query = "UPDATE carteiras " +
                "SET " + String.join(", ", updateFields) + " " +
                "WHERE id = ? AND id_usuario = ?";

        parameters.add(walletToUpdate.get("id"));
        parameters.add(walletToUpdate.get("id_usuario"));

        PreparedStatement stmt = conn.prepareStatement(query);
        for (int i = 0; i < parameters.size(); i++) {
            stmt.setObject(i + 1, parameters.get(i).toString());
        }

        int count = stmt.executeUpdate();

        if (count < 1) throw new InvalidParamsException("Carteira não encontrada", List.of());
        return get(Map.of("id", walletToUpdate.get("id"))).get(0);
    }

    @Override
    public void delete (Map<String, Object> params) throws SQLException, InvalidParamsException {
        ObjectMapper<WalletModel> objectMapper = new ObjectMapper<>(WalletModel.class);

        List<String> invalidFields = new ArrayList<>();
        for (String key : params.keySet()) {
            if (!objectMapper.hasField(key)) {
                invalidFields.add(key);
            }
        }
        if (!invalidFields.isEmpty()) throw new InvalidParamsException(invalidFields);

        StringBuilder query = new StringBuilder("DELETE FROM carteiras WHERE ");
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

        int count = stmt.executeUpdate();
        if (count < 1) throw new InvalidParamsException("Carteira não econtrada", List.of());
    }

    public WalletService(Connection conn) { this.conn = conn; }

    private final Connection conn;
}
