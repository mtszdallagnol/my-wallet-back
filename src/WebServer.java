import com.sun.net.httpserver.HttpServer;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class WebServer {
    private static final int PORT = 8080;
    private static final String DB_LOCATION;
    private static final String DB_USER;
    private static final String DB_PASSWORD;

    // Tamanho da pool de threads das req HTTP
    private static final int CORE_HTTP_THREADS = Runtime.getRuntime().availableProcessors();
    private static final int MAX_HTTP_THREADS = CORE_HTTP_THREADS * 2;
    private static final int HTTP_QUEUE_SIZE = 50;

    // Tamanho da pool de threads das req do DB
    private static final int CORE_DB_THREADS = Math.max(2, CORE_HTTP_THREADS / 2);
    private static final int MAX_DB_THREADS = CORE_HTTP_THREADS;
    private static final int DB_QUEUE_SIZE = 25;

    //?????JAVA????? dar uma estada**
    private static final ExecutorService dbExecutor = new ThreadPoolExecutor(
            CORE_DB_THREADS,
            MAX_DB_THREADS,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingDeque<>(DB_QUEUE_SIZE),
            new ThreadPoolExecutor.AbortPolicy()
    );

    //wtfk - static scope kkk
    static {
        String dbLocation = null, dbUser = null, dbPassword = null;

        Properties prop = new Properties();
        try (InputStream input = new FileInputStream("src/.env")) {
            prop.load(input);
            dbLocation = prop.getProperty("DB_LOCATION");
            dbUser = prop.getProperty("DB_USER");
            dbPassword = prop.getProperty("DB_PASSWORD");

            if (dbLocation == null || dbUser == null || dbPassword == null) {
                throw new IOException("Incorretas ariáveis em arquivo ENV");
            }
        } catch (IOException e) {
            System.err.println("Falha ao carregar configuração do banco de dados: " + e.getMessage());
            System.exit(1);
        }

        DB_LOCATION = dbLocation;
        DB_USER = dbUser;
        DB_PASSWORD = dbPassword;
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", PORT), 0);
        //criar contextos do db

        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
                CORE_HTTP_THREADS,
                MAX_HTTP_THREADS,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(HTTP_QUEUE_SIZE),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        server.setExecutor(threadPool);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Desligando servidor...");
            server.stop(0);
            threadPool.shutdown();
            dbExecutor.shutdown();
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

        server.start();
        System.out.println("Servidor iniciado na porta: " + PORT);
    }
}