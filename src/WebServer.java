import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;

public class WebServer {
    public static HttpServer _Server;

    private static final int PORT;
    private static final String DB_LOCATION;
    private static final String DB_USER;
    private static final String DB_PASSWORD;

    private static final int CORE_HTTP_THREADS = Runtime.getRuntime().availableProcessors();
    private static final int MAX_HTTP_THREADS = CORE_HTTP_THREADS * 2;
    private static final int HTTP_QUEUE_SIZE = 50;

    private static final int CORE_DB_THREADS = Math.max(2, CORE_HTTP_THREADS / 2);
    private static final int MAX_DB_THREADS = CORE_HTTP_THREADS;
    private static final int DB_QUEUE_SIZE = 25;

    //Revisar
    private static final ExecutorService dbExecutor = new ThreadPoolExecutor(
            CORE_DB_THREADS,
            MAX_DB_THREADS,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingDeque<>(DB_QUEUE_SIZE),
            new ThreadPoolExecutor.AbortPolicy()
    );

    private static final int DB_POOL_SIZE = Math.max(2, CORE_HTTP_THREADS * 2);
    private static final BlockingQueue<Connection> connectionPool = new LinkedBlockingQueue<>(DB_POOL_SIZE);
    private static final Object poolLock = new Object();

    //Revisar
    static {
        String dbLocation = null, dbUser = null, dbPassword = null;
        int port = -1;

        Properties prop = new Properties();
        try (InputStream input = new FileInputStream("src/.env")) {
            prop.load(input);
            dbLocation = prop.getProperty("DB_LOCATION");
            dbUser = prop.getProperty("DB_USER");
            dbPassword = prop.getProperty("DB_PASSWORD");
            port = Integer.parseInt(prop.getProperty("PORT"));

            if (dbLocation == null || dbUser == null || dbPassword == null || port == -1) {
                throw new IOException("Incorretas variáveis em arquivo ENV");
            }
        } catch (IOException e) {
            System.err.println("Falha ao carregar configuração do banco de dados: " + e.getMessage());
            System.exit(1);
        }

        DB_LOCATION = dbLocation;
        DB_USER = dbUser;
        DB_PASSWORD = dbPassword;
        PORT = port;

        try {
            for (int i = 0; i < DB_POOL_SIZE; i++) {
                connectionPool.put(DriverManager.getConnection(DB_LOCATION, DB_USER, DB_PASSWORD));
            }
        } catch (SQLException | InterruptedException e) {
            System.err.println("Falha ao inicializar conexões com banco de dados: " + e.getMessage());
            System.exit(1);
        }
    }

    public static void main(String[] args) throws IOException {
        _Server = HttpServer.create(new InetSocketAddress("127.0.0.1", PORT), 0);

        //criar contextos e handlers do db
        _Server.createContext("/users", new GeneralHandler());


        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
                CORE_HTTP_THREADS,
                MAX_HTTP_THREADS,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(HTTP_QUEUE_SIZE),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        _Server.setExecutor(threadPool);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Desligando servidor...");
            _Server.stop(0);
            threadPool.shutdown();
            dbExecutor.shutdown();
            for (Connection conn : connectionPool) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("Erro ao fechar conexão com banco de dados: " + e.getMessage());
                }
            }
            connectionPool.clear();
            try {
                if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    threadPool.shutdown();
                }
                if (!dbExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    dbExecutor.shutdown();
                }
            } catch (InterruptedException e) {
                threadPool.shutdown();
                dbExecutor.shutdown();
            }
            System.out.println("Servidor Desligado com sucesso");
        }));

        _Server.start();
        System.out.println("Servidor iniciado na porta: " + PORT);
    }

    private static void SendResponse (HttpExchange exchange, int responseCode, String response) {
        try {
            exchange.sendResponseHeaders(responseCode, response.length());
            OutputStream output = exchange.getResponseBody();
            output.write(response.getBytes());
            output.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private static class GeneralHandler implements HttpHandler {
        private static List<String> paths = List.of("/users");

        public void handle(HttpExchange exchange) throws IOException {
            String requestMethod = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            if (!paths.contains(path)) {
                SendResponse(exchange, 404, "Caminho não existente");
            }
            String tableName = path.substring(path.lastIndexOf("/") + 1);

            Executor executor = exchange.getHttpContext().getServer().getExecutor();
            if (!(executor instanceof ThreadPoolExecutor)) {
                System.err.println("Thread atual não faz parte da thread pool, sem comportamento ASYNC");
            }

            SendResponse(exchange, 500, "SUcesso");

            if (requestMethod == "GET") {

            } else if (requestMethod == "POST") {

            } else if (requestMethod == "PUT") {

            } else {

            }
        }
    }
}