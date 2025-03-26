import Server.WebServer;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try {
            WebServer.start();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
