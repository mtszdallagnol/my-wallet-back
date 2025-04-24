package Wallets;

import Exceptions.MappingException;
import Exceptions.ValidationException;
import General.ObjectMapper;
import Interfaces.ServiceInterface;

import java.math.BigInteger;
import java.sql.*;
import java.util.*;

import Exceptions.InvalidParamsException;

public class WalletService implements ServiceInterface<WalletModel> {

    @Override
    public List<WalletModel> get(Map<String, Object> params) throws Exception{
        ObjectMapper<WalletModel> objectMapper = new ObjectMapper<>(WalletModel.class);

        List<String> invalidFields = new ArrayList<>();

        for(String key : params.keySet()){
            if(!objectMapper.hasField(key)){
                invalidFields.add(key);
            }
        }

        if(!invalidFields.isEmpty()) throw new InvalidParamsException("Paramêtro(s) inválido(s)", invalidFields);

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
            throw new MappingException("Falha ao mapear objeto", errors);
        }

        return response;
    }

    @Override
    public Optional<WalletModel> post(Map<String, Object> walletToPost) throws SQLException {
        ObjectMapper<WalletModel> objectMapper = new ObjectMapper<>(WalletModel.class);

        List<String> invalidFields = new ArrayList<>();
        for (String key : walletToPost.keySet()){
            if(!objectMapper.hasField(key) && !key.equals("id_usuario")){
                invalidFields.add(key);
            }
        }
        if(!invalidFields.isEmpty()) throw new InvalidParamsException("Parâmetro(s) inválido(s)", invalidFields);

        WalletModel wallet = objectMapper.map(walletToPost, conn);

        List<String> errors = objectMapper.getErrors();
        if(!errors.isEmpty()){
            throw new MappingException("Falha ao mapear objeto", errors);
        }

        List<String> validationErrors = objectMapper.getValidationErrors();
        if(!validationErrors.isEmpty()){
            throw new ValidationException("Falha de validação", validationErrors);
        }

        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO carteiras (nome, descricao, id_usuario)" +
                        "VALUES (?, ?, ?);",
                Statement.RETURN_GENERATED_KEYS);

        stmt.setString(1, wallet.getNome());
        stmt.setString(2, wallet.getDescricao());
        stmt.setInt(3, wallet.getId_usuario());

        stmt.executeUpdate();

        ResultSet generatedKeys = stmt.getGeneratedKeys();

        if (generatedKeys.next()) {
            Object value = generatedKeys.getObject("GENERATED_KEY");
            wallet.setId(((BigInteger) value).intValue());
        }

        return Optional.of(wallet);
    }

    public Optional<WalletModel> update(Map<String, Object> params) throws Exception{
        return Optional.empty();
    }

    public void delete(Map<String, Object> params) throws Exception{

    }

    public WalletService(Connection conn) { this.conn = conn; }

    private final Connection conn;
}
