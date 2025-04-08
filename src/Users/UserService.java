package Users;

import General.ObjectMapper;
import General.ServiceInterface;
import Responses.ServiceResponse;
import Server.WebServer;

import java.security.Provider;
import java.sql.*;
import java.util.*;

public class UserService implements ServiceInterface<UserDTO> {
    @Override
    public ServiceResponse<List<UserDTO>> getAll() throws Exception {
        // Sempre retorne ServiceResponse o que varia e o dentro nesse caso é uma lista
        ServiceResponse<List<UserDTO>> response = new ServiceResponse<>();
        // Instancie o objeto de mapeamento de classe em contexto da classe atual, nesse caso UserDTO
        ObjectMapper<UserDTO> objectMapper = new ObjectMapper<>(UserDTO.class);

        // Pegue uma conexão com o bando de dados
        Connection conn = WebServer.databaseConnectionPool.getConnection();
        // Prepare a string sql, isso é importante principalmente com variaveis para sanatizar as paradas
        PreparedStatement stmt = conn.prepareStatement(
                "SELECT * FROM usuarios"
        );
        // Executa a query preparada na linha anterior e retorna um ponteiro para o inicio dos resultados
        ResultSet rs = stmt.executeQuery();

        // Baseado no ponteiro analisa os dados subsequentes para adquirir certos meta dados especificos do banco
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();


        // Essa sequencia inteira passa por cada resultado do banco e os mapeia um por um em um objeto UserDTO ->
        // pelo object mapper
        List<UserDTO> users = new ArrayList<>();
        while(rs.next()) {
            Map<String, Object> row = new HashMap<>();

            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                Object value = rs.getObject(i);
                row.put(columnName, value);
            }

            users.add(objectMapper.map(row));
        }

        // Verifica se algum erro aconteceu durante o processo de mapeamento dos dados do bando para objeto java ->
        // estamos retornando para o usuario o erro (so para n derrubar o servidor) mas o certo e n ter erro nenhum ->
        // nessa etapa
        List<String> errors = objectMapper.getErrors();
        if (!errors.isEmpty()) {
            response.isSuccessful = false;
            response.msg = "Erro ao mapear objeto do banco de dados: " + String.join(", ", errors);

            return response;
        }

        response.isSuccessful = true;
        response.data = users;

        // SEMPRE que pegar uma conexão com o banco retorne-a quando terminar se não o servidor explode >=(
        WebServer.databaseConnectionPool.returnConnection(conn);
        return response;
    }

    @Override
    public ServiceResponse<UserDTO> getById(int id) throws Exception {
        // Sempre retorne ServiceResponse o que varia e o dentro nesse caso e so o UserDTO
        ServiceResponse<UserDTO> response = new ServiceResponse<>();
        ObjectMapper<UserDTO> objectMapper = new ObjectMapper<>(UserDTO.class);

        // Pegue uma conexão com o bando de dados
        Connection conn = WebServer.databaseConnectionPool.getConnection();
        // Prepare a string sql, isso é importante principalmente com variaveis para sanatizar as paradas
        PreparedStatement stmt = conn.prepareStatement(
                "SELECT * FROM usuarios WHERE id = ?"
        );

        // Define a primeira wildCard ? como id de maneira sanatizada
        stmt.setInt(1, id);

        // Executa a query preparada na linha anterior e retorna um ponteiro para o inicio dos resultados
        ResultSet rs = stmt.executeQuery();

        // Baseado no ponteiro analisa os dados subsequentes para adquirir certos meta dados especificos do banco
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        // Instanciamos o usuarios de resposta
        UserDTO user = null;
        // Essa sequencia inteira passa por cada resultado do banco e os mapeia um por um em um objeto UserDTO ->
        // pelo object mapper
        // verificamos somente um paço no array do banco pois no melhor cenario existe somente um registro
        if (rs.next()) {
            Map<String, Object> row = new HashMap<>();

            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                Object value = rs.getObject(columnName);
                row.put(columnName, value);
            }

            user = objectMapper.map(row);
        } else {
            // Se na primeira e unica verificação não existe nenhum registro nao existe registro com referente id ->
            response.isSuccessful = false;
            response.msg = "Nenhum registro com referente ID";

            return response;
        }

        // Verifica se algum erro aconteceu durante o processo de mapeamento dos dados do bando para objeto java ->
        // estamos retornando para o usuário o erro (so para n derrubar o servidor) mas o certo e n ter erro nenhum ->
        // nessa etapa
        List<String> errors = objectMapper.getErrors();
        if (!errors.isEmpty()) {
            response.isSuccessful = false;
            response.msg = "Erro ao mapear objeto do banco de dados: " + String.join(", ", errors);

            return response;
        }

        response.isSuccessful = true;
        response.data = user;

        // SEMPRE que pegar uma conexão com o banco retorne-a quando terminar se não o servidor explode >=(
        WebServer.databaseConnectionPool.returnConnection(conn);
        return response;
    }

    @Override
    public ServiceResponse post(UserDTO user) {
        return null;
    }

    @Override
    public ServiceResponse update(UserDTO user) {
        return null;
    }

    @Override
    public ServiceResponse<Void> delete (int id) throws Exception {
        // Sempre retorne ServiceResponse o que varia e o dentro nesse caso e so o que não ->
        // retornamos nenhum dado do banco
        ServiceResponse<Void> response = new ServiceResponse<>();

        // Pegue uma conexão com o bando de dados
        Connection conn = WebServer.databaseConnectionPool.getConnection();
        // Prepare a string sql, isso é importante principalmente com variaveis para sanatizar as paradas
        PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM usuarios WHERE id=?"
        );

        // Define a primeira wildCard ? como id de maneira sanatizada
        stmt.setInt(1, id);

        // Pega o numero de linhas alteradas no banco na execucao da query preparada
        int rowCount = stmt.executeUpdate();

        response.isSuccessful = rowCount > 0;
        response.rowChanges = rowCount;

        // SEMPRE que pegar uma conexão com o banco retorne-a quando terminar se não o servidor explode >=(
        WebServer.databaseConnectionPool.returnConnection(conn);
        return response;
    }
}
