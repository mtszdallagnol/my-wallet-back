package General;

import java.util.logging.*;

public class ColoredLogger {
    private static final Logger logger = Logger.getLogger("DefaultLogger");

    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
//    private static final String GREEN = "\u001B[32m";
    private static final String BLUE = "\u001B[34m";

    static {
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new Formatter() {
            @Override
            public String format(LogRecord record) {
                String color;
                if (record.getLevel() == Level.SEVERE) {
                    color = RED;
                } else if (record.getLevel() == Level.WARNING) {
                    color = YELLOW;
                } else if (record.getLevel() == Level.INFO) {
                    color = BLUE;
                } else {
                    color = "\\e[0;37m";
                }

                return color + record.getLevel() + ": " + record.getMessage() + RESET + "\n";
            }
        });

        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
    }

    public static Logger getLogger() {
        return logger;
    }
}
