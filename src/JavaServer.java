import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class JavaServer {
    private static final int PORT = 8080;
    private static final String DB_URL;
    private static final String DB_USER;
    private static final String DB_PASSWORD;

    static {
        String url = "default.url";
        String user = "default.user";
        String password = "default.password";

        try {
            List<String> lines = Files.readAllLines(Paths.get("src/.env"));
            for (String line : lines) {
                String[] temp = line.split(" ");
                if (temp[0].equals("DB_LOCATION"))
                    url = temp[1];
                else if (temp[0].equals("DB_USER"))
                    user = temp[1];
                else
                    password = temp[1];
            }
        } catch (IOException e) {
            System.err.println("Erro ao ler arquivo ENV: " + e.getMessage());
            e.printStackTrace();
        }
        DB_URL = url;
        DB_USER = user;
        DB_PASSWORD = password;
    }
    
}
