import Server.WebServer;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try {
            WebServer.start();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
