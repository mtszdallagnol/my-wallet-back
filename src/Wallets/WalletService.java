package Wallets;

import Exceptions.MappingException;
import Exceptions.ValidationException;
import General.ObjectMapper;
import Interfaces.ServiceInterface;

import java.math.BigInteger;
import java.sql.*;
import java.util.*;

import Exceptions.InvalidParamsException;
import Users.UserDTO;
import Users.UserModel;

import javax.xml.transform.Result;

public class WalletService implements ServiceInterface<WalletModel> {

    @Override
    public List<WalletModel> get(Map<String, Object> params) throws InvalidParamsException, SQLException {
        ObjectMapper<WalletModel> objectMapper = new ObjectMapper<>(WalletModel.class);

        List<String> invalidFields = new ArrayList<>();

        for(String key : params.keySet()){
            if(!objectMapper.hasField(key)){
                invalidFields.add(key);
            }
        }

        if(!invalidFields.isEmpty()) throw new InvalidParamsException(invalidFields);

        StringBuilder query = new StringBuilder("SELECT * FROM carteiras");

        if(!params.isEmpty()){
            query.append(" WHERE ");

            for(String key : params.keySet()){
                String temp = key + " = ? AND ";
                query.append(temp);
            }

            query.delete(query.length() - 5, query.length());
            query.append(";");
        }

        PreparedStatement stmt = conn.prepareStatement(query.toString());

        if(!params.isEmpty()){
            int enumerator = 1;

            for(Object value : params.values()){
                stmt.setObject(enumerator, value);
                enumerator++;
            }
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

            response.add(objectMapper.map(row));
        }

        List<String> errors = objectMapper.getErrors();
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
        ObjectMapper<WalletDTO.updateRequirementModel> objectMapper = new ObjectMapper<>(WalletDTO.updateRequirementModel.class);

        if (walletToUpdate.size() < 2) throw new InvalidParamsException("Nenhum parâmetro enviado", List.of());

        List<String> invalidFields = new ArrayList<>();
        for (String key : walletToUpdate.keySet()) {
            if (!objectMapper.hasField(key)) {
                invalidFields.add(key);
            }
        }
        if (!invalidFields.isEmpty()) throw new InvalidParamsException(invalidFields);

        if (walletToUpdate.size() < 2) throw new InvalidParamsException("Nenhum parâmetro enviado", List.of());

        List<String> validationErrors = objectMapper.executeValidation(walletToUpdate, conn);
        if (!validationErrors.isEmpty()) throw new ValidationException(validationErrors);

        List<String> errors = objectMapper.getErrors();
        if (!errors.isEmpty()) throw new MappingException(errors);

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
