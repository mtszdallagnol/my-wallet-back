package Server;

import Auth.AuthController;
import Responses.ControllerResponse;
import Users.UserService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.logging.Logger;

import General.ColoredLogger;
import Users.UserController;
import General.JsonParsers;

public class WebServer {
    private static HttpServer _Server = null;
    public static DatabaseConnectionPool databaseConnectionPool;
    private static String ADDRESS = null;
    private static int PORT = -1;
    public static String JWT_SECRET_KEY = null;

    public static final ExecutorService dbThreadPool = new ThreadPoolExecutor(
            Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
            Runtime.getRuntime().availableProcessors(),
            60L, TimeUnit.SECONDS,
            new LinkedBlockingDeque<>(25),
            new ThreadPoolExecutor.AbortPolicy()
    );

    private static final Logger logger = ColoredLogger.getLogger();

    public static void start() throws IOException, SQLException, Exception {
        if (_Server != null) {
            logger.warning("Servidor já iniciado");
            return;
        }

        Properties prop = new Properties();
        InputStream input = new FileInputStream("src/.env");
        prop.load(input);

        ADDRESS = prop.getProperty("ADDRESS");
        PORT = Integer.parseInt(prop.getProperty("PORT"));
        JWT_SECRET_KEY = prop.getProperty("JWT_SECRET_KEY");

        _Server = HttpServer.create(new InetSocketAddress(ADDRESS, PORT), 0);

        _Server.createContext("/users", exchange -> {
            Connection conn = null;
            try {
                conn = databaseConnectionPool.getConnection();
                new UserController().handle(exchange, conn);}
            catch (Exception e) { throw new RuntimeException(e); }
            finally { if (conn != null) databaseConnectionPool.returnConnection(conn); } });
        _Server.createContext("/auth", exchange -> {
            Connection conn = null;
            try {
                conn = databaseConnectionPool.getConnection();
                new AuthController().handle(exchange, conn); }
            catch (Exception e) { throw new RuntimeException(e); }
            finally { if (conn != null) databaseConnectionPool.returnConnection(conn); }
        });

        ThreadPoolExecutor httpThreadPool = new ThreadPoolExecutor(
                Runtime.getRuntime().availableProcessors(),
                Runtime.getRuntime().availableProcessors() * 2,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(50),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        _Server.setExecutor(httpThreadPool);

        databaseConnectionPool = DatabaseConnectionPool.getInstance();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Desligando servidor...");
            _Server.stop(0);

            databaseConnectionPool.shutdown();

            httpThreadPool.shutdown();
            dbThreadPool.shutdown();
            try {
                if (!httpThreadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    httpThreadPool.shutdownNow();
                }
                if (!dbThreadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    dbThreadPool.shutdown();
                }
            } catch (InterruptedException e) {
                httpThreadPool.shutdownNow();
                dbThreadPool.shutdownNow();
            }
        }));

        _Server.start();
        logger.info("Servidor inciado no endereço: " + ADDRESS + ":" + PORT);
    }


    public static void SendResponse(HttpExchange exchange, ControllerResponse response) throws IOException {
        String finalResponse = "{\"error\": 1, \"httpStatus\": 500, \"msg\": \"Erro ao serializar resposta para JSON\", \"data\": null}";

        JsonParsers.SerializationResult result = JsonParsers.serialize(response);
        if (result.isSuccess()) {
            finalResponse = result.getJsonString();
        } else {
            System.out.println(result.getError().getMessage());
        }

        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(response.httpStatus, finalResponse.getBytes(StandardCharsets.UTF_8).length);
        OutputStream os = exchange.getResponseBody();
        os.write(finalResponse.getBytes(StandardCharsets.UTF_8));
        os.close();
    }
}
