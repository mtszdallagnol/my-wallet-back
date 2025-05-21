package Transactions;

import Assets.AssetsDTO;
import Assets.AssetsModel;
import Assets.AssetsService;
import Exceptions.InvalidParamsException;
import Exceptions.MappingException;
import Exceptions.ValidationException;
import General.ObjectMapper;
import Interfaces.ServiceInterface;
import Users.UserModel;
import Wallets.WalletModel;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;


public class TransactionService implements ServiceInterface<TransactionModel> {

    @Override
    public List<TransactionModel> get(Map<String, Object> params) throws Exception{
        ObjectMapper<TransactionModel> tMapper = new ObjectMapper<>(TransactionModel.class);
        ObjectMapper<WalletModel> wMapper = new ObjectMapper<>(WalletModel.class);
        ObjectMapper<UserModel> uMapper = new ObjectMapper<>(UserModel.class);

        List<String> invalidFields = params.keySet().stream()
                .filter(key -> !tMapper.hasField(key)
                                && !wMapper.hasField(key)
                                && !uMapper.hasField(key))
                .collect(Collectors.toList());
        if (!invalidFields.isEmpty()) throw new InvalidParamsException(invalidFields);

        List<String> tKeys = new ArrayList<>();
        List<String> wKeys = new ArrayList<>();
        List<String> uKeys = new ArrayList<>();

        for (String key : params.keySet()) {
            if      (tMapper.hasField(key)) tKeys.add(key);
            else if (wMapper.hasField(key)) wKeys.add(key);
            else                            uKeys.add(key);
        }

        StringBuilder query = new StringBuilder(
                "SELECT t.* FROM transacoes t"
        );

        if (!wKeys.isEmpty() || !uKeys.isEmpty()) query.append(" INNER JOIN carteiras c ON c.id = t.id_carteira");
        if (!uKeys.isEmpty()) query.append(" INNER JOIN usuarios u ON u.id = c.id_usuario");

        List<String> clauses   = new ArrayList<>();
        List<Object> arguments = new ArrayList<>();

        for (String key : tKeys) {
            arguments.add(params.get(key));

            key = key.contains(".") ? key.substring(key.indexOf(".") + 1) : key;
            clauses.add("t." + key + " = ?");
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

        List<TransactionModel> response = new ArrayList<>();
        while(rs.next()) {
            Map<String, Object> row = new HashMap<>();

            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                Object value = rs.getObject(i);

                row.put(columnName, value);
            }

            response.add(tMapper.map(row));
        }

        List<String> errors = tMapper.getErrors();
        if (!errors.isEmpty()) throw new MappingException(errors);

        return response;
    }

    @Override
    public TransactionModel post(Map<String, Object> transactionToPost) throws Exception {
        ObjectMapper<TransactionDTO.postRequirementModel> objectMapper = new ObjectMapper<>(TransactionDTO.postRequirementModel.class);

        List<String> invalidFields = new ArrayList<>();
        for (String key : transactionToPost.keySet()) {
            if (!objectMapper.hasField(key)) {
                invalidFields.add(key);
            }
        }
        if (!invalidFields.isEmpty()) throw new InvalidParamsException(invalidFields);

        List<String> validationErrors = objectMapper.executeValidation(transactionToPost, conn);
        if (!validationErrors.isEmpty()) throw new ValidationException(validationErrors);

        List<String> errors = objectMapper.getErrors();
        if (!errors.isEmpty()) throw new MappingException(errors);

        transactionToPost.remove("id_usuario");

        AssetsService assetsService = new AssetsService(conn);
        AssetsModel referencedAsset = assetsService.get(Map.of("nome", transactionToPost.get("nome_ativo"))).get(0);

        transactionToPost.put("id_ativo", referencedAsset.getId());
        transactionToPost.remove("nome_ativo");

        BigDecimal assetQuantity  = (BigDecimal) transactionToPost.get("quantidade");
        if (referencedAsset.getTipo() == AssetsDTO.assetType.ACAO &&
           assetQuantity.stripTrailingZeros().scale() > 0 ) {

            throw new ValidationException(List.of("quantidade: " + "Quantidade incompatível com tipo de ativo (" + referencedAsset.getTipo() + ")"));
        }

        BigDecimal totalValue = assetQuantity.multiply((BigDecimal) transactionToPost.get("valor_unitario"));
        if (transactionToPost.containsKey("taxa_corretagem"))
            totalValue = totalValue.subtract(totalValue.multiply((BigDecimal) transactionToPost.get("taxa_corretagem")));
        transactionToPost.put("valor_total", totalValue);

        String columns = String.join(", ", transactionToPost.keySet());
        String placeholder = String.join(", ", Collections.nCopies(transactionToPost.size(), "?"));

        String query = "INSERT INTO transacoes (" + columns + ") VALUES (" + placeholder + ")";

        PreparedStatement stmt = conn.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS);

        int enumerator = 1;
        for (Object value : transactionToPost.values()) {
            if (value == null) stmt.setNull(enumerator++, Types.NULL);
            else stmt.setString(enumerator++, value.toString());
        }

        stmt.executeUpdate();

        ResultSet generatedKeys = stmt.getGeneratedKeys();

        generatedKeys.next();

        Object id = generatedKeys.getObject(1);
        return get(Map.of("id", id)).get(0);
     }

    @Override
    public TransactionModel update(Map<String, Object> userToUpdate) throws Exception {
        return null;
    }

    @Override
    public void delete (Map<String, Object> params) throws Exception{

    }

    public TransactionService(Connection conn) { this.conn = conn; }

    private final Connection conn;
}
