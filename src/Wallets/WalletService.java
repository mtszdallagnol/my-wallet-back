package Wallets;

import Exceptions.MappingException;
import General.ObjectMapper;
import Interfaces.ServiceInterface;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.*;

import Exceptions.InvalidParamsException;

public class WalletService implements ServiceInterface<WalletModel> {

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

        List<String> erros = objectMapper.getErrors();
        if(!erros.isEmpty()){
            throw new MappingException("Falha ao mapear objeto", erros);
        }

        return response;
    }

    public Optional<WalletModel> post(Map<String, Object> userToPost) throws Exception{
        return Optional.empty();
    }

    public Optional<WalletModel> update(Map<String, Object> params) throws Exception{
        return Optional.empty();
    }

    public void delete(Map<String, Object> params) throws Exception{

    }

    public WalletService(Connection conn) { this.conn = conn; }

    private final Connection conn;
}
