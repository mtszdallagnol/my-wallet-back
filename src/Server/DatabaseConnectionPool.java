package Server;

import General.ColoredLogger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class DatabaseConnectionPool {
    private static DatabaseConnectionPool instance;

    private static final Logger logger = ColoredLogger.getLogger();

    private static String DB_LOCATION = null;
    private static String DB_USER = null;
    private static String DB_PASSWORD = null;

    private final int MAX_CONNECTIONS = 10;
    private final int MIN_CONNECTIONS = 5;
    private final long CONNECTION_TIMEOUT = 30 * 1000;
    private final long MAX_CONNECTION_AGE = 30 * 60 * 1000;
    private final long CONNECTION_IDLE_TIMEOUT = 10 * 60 * 1000;
    private final long VALIDATION_INTERVAL = 5 * 60 * 1000;

    private final int MAX_CONNECTION_RETRIES = 3;
    private final long BASE_RETRY_DELAY_MS = 1 * 1000;
    private final double RETRY_BACKOFF_FACTOR = 2.0;

    private final CopyOnWriteArrayList<PooledConnection> connectionPool = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<PooledConnection> activeConnection = new CopyOnWriteArrayList<>();

    private final ScheduledExecutorService maintenanceExecutor;

    private class PooledConnection {
        private Connection connection;
        private long creationTime;
        private long lastValidationTime;
        private volatile long lastUsageTime;
        private volatile boolean inUse;

        public PooledConnection() throws SQLException {
            connection = createConnection();
            creationTime = System.currentTimeMillis();
            lastValidationTime = creationTime;
            lastUsageTime = creationTime;
        }

        private Connection createConnection() throws SQLException {
            SQLException lastException = null;

            for (int i = 0; i <= MAX_CONNECTION_RETRIES; i++) {
                try {
                    Connection conn = DriverManager.getConnection(DB_LOCATION, DB_USER, DB_PASSWORD);
                    validateConnection(conn);

                    return conn;
                } catch (SQLException e) {
                    lastException = e;
                    if (isCriticalConnectionError(e)) {
                        logger.severe("Erro crítico ao realizar conexão com banco de dados: " + e.getCause());
                        throw e;
                    }

                    long delay = (long) (BASE_RETRY_DELAY_MS * Math.pow(RETRY_BACKOFF_FACTOR, i - 1));
                    logger.warning("Tentativa de conexão: " + i + " falhou. Tentando novamente em " + delay + "ms: " + e.getMessage());

                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new SQLException("Reconexão interrompida", ie);
                    }
                }
            }

            if (lastException != null) {
                logger.severe("Falha ao estabelecer conexão com bando de dados depois de: " + MAX_CONNECTION_RETRIES + "tentativas: " + lastException.getCause());
                throw lastException;
            }

            throw new SQLException("Erro inesperado no mecanismo de retentativa de conexão");
        }

        private void validateConnection(Connection conn) throws SQLException {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT 1")) {
                stmt.executeQuery();
            }
        }

        public Connection getConnection() throws SQLException {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastValidationTime > VALIDATION_INTERVAL) {
                validateConnection(connection);
                lastValidationTime = currentTime;
            }

            inUse = true;
            lastUsageTime = currentTime;
            return connection;
        }

        public void releaseConnection() {
            inUse = false;
        }

        public boolean isExpired() {
            long currentTime = System.currentTimeMillis();
            return (currentTime - creationTime > MAX_CONNECTION_AGE) ||
                    (!inUse && currentTime - lastUsageTime > CONNECTION_IDLE_TIMEOUT);
        }

        public boolean needsValidation() {
            return System.currentTimeMillis() - lastValidationTime > VALIDATION_INTERVAL;
        }

        public void close() throws SQLException {
            if (connection != null & !connection.isClosed()) {
                connection.close();
            }
        }

        public boolean isClosed() throws SQLException {
            return connection == null || connection.isClosed();
        }

        private boolean isCriticalConnectionError(SQLException e) {
            String sqlState = e.getSQLState();
            int errorCode = e.getErrorCode();

            return (sqlState != null && (
                    sqlState.startsWith("08") ||
                    sqlState.startsWith("57") ||
                    errorCode == 0
                    ));
        }
    }

    private DatabaseConnectionPool() throws IOException, SQLException, Exception {
        Properties prop = new Properties();

        InputStream input = new FileInputStream("src/.env");
        prop.load(input);

        DB_LOCATION = prop.getProperty("DB_LOCATION");
        DB_USER = prop.getProperty("DB_USER");
        DB_PASSWORD = prop.getProperty("DB_PASSWORD");

        if (DB_LOCATION == null || DB_USER == null || DB_PASSWORD == null) {
            throw new IOException("Falta de parâmetros de configuração de banco de dados (ARQUIVO .ENV)");
        }

        initializeConnectionPool();

        maintenanceExecutor= Executors.newScheduledThreadPool(2);

        maintenanceExecutor.scheduleAtFixedRate(() -> {
            try {
                maintainConnectionPool();
            } catch (Exception e) {
                logger.severe("Erro ao manter as conexões: " + e.getMessage());
                System.exit(1);
            }
        }, 60, 60, TimeUnit.SECONDS);
        maintenanceExecutor.scheduleAtFixedRate(() -> {
            try {
                validateConnections();
                System.exit(1);
            } catch (SQLException e) {
                logger.severe("Erro na validação das conexões: " + e.getMessage());
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    private synchronized void initializeConnectionPool() throws Exception {
        for (int i = 0; i < MIN_CONNECTIONS; i++) {
            connectionPool.add(new PooledConnection());
        }

        if (connectionPool.isEmpty()) {
            logger.severe("Falha ao criar qualquer conexão com o banco de dados");
            throw new Exception();
        }
    }

    private synchronized void maintainConnectionPool() throws SQLException, Exception {
        try {
            connectionPool.removeIf(PooledConnection::isExpired);
            activeConnection.removeIf(PooledConnection::isExpired);

            for (int i = 0; (connectionPool.size() < MIN_CONNECTIONS) || i == MAX_CONNECTION_RETRIES; i++) {
                connectionPool.add(new PooledConnection());
            }

            if (connectionPool.isEmpty()) {
                throw new SQLException("Falha ao manter número minimo de conexão com banco de dados");
            }

            for (int i = 0; (connectionPool.size() > MAX_CONNECTIONS) || i == MAX_CONNECTION_RETRIES; i++) {
                try {
                    PooledConnection conn = connectionPool.remove(0);
                    conn.close();
                } catch (SQLException e) {
                    logger.warning("Falha ao fechar conexão com banco de dados");
                }
            }

            if (connectionPool.size() > MAX_CONNECTIONS) {
                throw new SQLException("Falha ao manter número máximo de conexões de banco de dados");
            }
        } catch (Exception e) {
            logger.severe("Falha inesperada ao realizar a manutenção da piscina de conexões.");
            throw new Exception();
        }
    }

    private synchronized void validateConnections() throws SQLException {
        int invalidConnectionsCount = 0;
        for (PooledConnection conn : connectionPool) {
            if (conn.needsValidation()) {
                try {
                    conn.validateConnection(conn.getConnection());
                } catch (SQLException e) {
                    logger.warning("Falha ao realizar verificação de conexões: " + e.getMessage());
                    try {
                        conn.close();
                        connectionPool.remove(conn);
                    } catch (SQLException sqlE) {
                        logger.warning("Falha ao fechar conexão inválida");
                        invalidConnectionsCount++;
                    }
                }
            }
        }
        if (invalidConnectionsCount > MAX_CONNECTION_RETRIES) {
            throw new SQLException("Falha ao controlar número de conexões inválidas");
        }
    }

    public synchronized Connection getConnection() throws SQLException {
        long startTime = System.currentTimeMillis();

        while (true) {
            if (System.currentTimeMillis() - startTime > CONNECTION_TIMEOUT) {
                throw new SQLException("Tempo limite de tentativa de conexão excedido");
            }

            for (PooledConnection conn : connectionPool) {
                if (!conn.inUse) {
                    connectionPool.remove(conn);
                    activeConnection.add(conn);
                    return conn.getConnection();
                }
            }

            if (connectionPool.size() + activeConnection.size() < MAX_CONNECTIONS) {
                try {
                    PooledConnection newConn = new PooledConnection();
                    activeConnection.add(newConn);
                    return newConn.getConnection();
                } catch (SQLException e) {
                    logger.warning("Falha ao criar nova conexão");
                }
            }

            try {
                wait(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SQLException("Recuperação de conexão interrompida", e);
            }
        }
    }

    public synchronized void returnConnection(Connection returnConn) {
        for (PooledConnection conn : activeConnection) {
            boolean equals = false;
            try {
                equals = conn.getConnection() == returnConn;
            } catch (SQLException e) {
                continue;
            }
            if (equals) {
                conn.releaseConnection();
                activeConnection.remove(conn);
                connectionPool.add(conn);
                break;
            }
        }
    }

    public void shutdow() {
        maintenanceExecutor.shutdown();

        CopyOnWriteArrayList<PooledConnection> allConnections = new CopyOnWriteArrayList<>(connectionPool);
        allConnections.addAll(activeConnection);

        for (PooledConnection conn : allConnections) {
            for (int i = 0; i < MAX_CONNECTION_RETRIES; i++) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    logger.warning("Erro ao fechar conexão");
                }
            }
        }
    }

    public static synchronized DatabaseConnectionPool getInstance() throws IOException, SQLException, Exception {
        if (instance == null) {
            instance = new DatabaseConnectionPool();
        }

        return instance;
    }
}