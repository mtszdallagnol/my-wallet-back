package Server;

import General.ResponseAPI;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.logging.Logger;

import General.ColoredLogger;
import Users.UserController;
import General.JsonSerializer;

public class WebServer {
    private static HttpServer _Server = null;

    private static String DB_LOCATION = null;
    private static String DB_USER = null;
    private static String DB_PASSWORD = null;
    private static String ADDRESS = null;
    private static int PORT = -1;

    public static final ExecutorService dbThreadPool = new ThreadPoolExecutor(
            Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
            Runtime.getRuntime().availableProcessors(),
            60L, TimeUnit.SECONDS,
            new LinkedBlockingDeque<>(25),
            new ThreadPoolExecutor.AbortPolicy()
    );

    private static final Logger logger = ColoredLogger.getLogger();

    private WebServer() throws IOException {
        Properties prop = new Properties();

        InputStream input = new FileInputStream("src/.env");
        prop.load(input);

        DB_LOCATION = prop.getProperty("DB_LOCATION");
        DB_USER = prop.getProperty("DB_USER");
        DB_PASSWORD = prop.getProperty("DB_PASSWORD");
        ADDRESS = prop.getProperty("ADDRESS");
        PORT = Integer.parseInt(prop.getProperty("PORT"));

        if (DB_LOCATION == null || DB_USER == null || DB_PASSWORD == null ||
            ADDRESS == null || PORT == -1) {
            logger.severe("Falha ao lidar com variáveis do arquivo ENV");
            throw new IOException();
        }
    }

    public static void start() throws IOException {
        if (_Server != null) {
            logger.warning("Servidor já iniciado");
            return;
        }

        new WebServer();

        _Server = HttpServer.create(new InetSocketAddress(ADDRESS, PORT), 0);

        _Server.createContext("/users", exchange -> { UserController.handle(exchange); } );

        ThreadPoolExecutor httpThreadPool = new ThreadPoolExecutor(
                Runtime.getRuntime().availableProcessors(),
                Runtime.getRuntime().availableProcessors() * 2,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(50),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        _Server.setExecutor(httpThreadPool);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Desligando servidor...");
            _Server.stop(0);

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


    public static void SendResponse(HttpExchange exchange, ResponseAPI response) throws IOException {
        String finalResponse = "{\"error\": 1, \"httpStatus\": 500, \"msg\": \"Erro ao serializar resposta para JSON\", \"data\": null}";

        JsonSerializer.SerializationResult result = JsonSerializer.serialize(response);
        if (result.isSuccess()) {
            finalResponse = result.getJsonString();
        }

        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(response.httpStatus, finalResponse.getBytes("UTF-8").length);
        OutputStream os = exchange.getResponseBody();
        os.write(finalResponse.getBytes("UTF-8"));
        os.close();
    }
}
