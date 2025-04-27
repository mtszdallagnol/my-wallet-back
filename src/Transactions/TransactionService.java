package Transactions;

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


public class TransactionService implements ServiceInterface<TransactionModel> {

    @Override
    public List<TransactionModel> get(Map<String, Object> params) throws Exception{
        ObjectMapper<TransactionModel> objectMapper = new ObjectMapper<>(TransactionModel.class);

        List<String> invalidFields = new ArrayList<>();

        for(String key : params.keySet()){
            if(!objectMapper.hasField(key)){
                invalidFields.add(key);
            }
        }

        if(!invalidFields.isEmpty()) throw new InvalidParamsException(invalidFields);

        StringBuilder query = new StringBuilder("SELEC * FROM transacoes");

        if(!params.isEmpty()){
            query.append("WHERE");

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

        ResultSet resultSet = stmt.executeQuery();

        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();

        List<TransactionModel> response = new ArrayList<>();
        while(resultSet.next()){
            Map<String, Object> row = new HashMap<>();

            for(int i = 1; i <= columnCount; i++){
                String columnName = metaData.getColumnName(i);
                Object value = resultSet.getObject(columnName);
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
    public TransactionModel post(Map<String, Object> userToPost) throws Exception{
        return null;
    }

    @Override
    public TransactionModel update(Map<String, Object> userToUpdate) throws Exception{
        return null;
    }

    @Override
    public void delete (Map<String, Object> params) throws Exception{

    }

    public TransactionService(Connection conn) { this.conn = conn; }

    private final Connection conn;
}
