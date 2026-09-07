package de.pixelrpg.rpg.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import javax.sql.DataSource;

public final class DatabaseManager {
    private static final int CURRENT_SCHEMA_VERSION = 2;
    private HikariDataSource dataSource;

    public void connect(FileConfiguration config) {
        String host = requireHost(config.getString("storage.mysql.host", "localhost"));
        int port = config.getInt("storage.mysql.port", 3306);
        if (port < 1 || port > 65535) throw new IllegalArgumentException("storage.mysql.port must be between 1 and 65535");
        String database = requireIdentifier(config.getString("storage.mysql.database", "pixelrpg"), "storage.mysql.database");
        String username = config.getString("storage.mysql.username", "root");
        String password = config.getString("storage.mysql.password", "");
        int poolSize = Math.clamp(config.getInt("storage.mysql.pool-size", 10), 2, 64);
        long connectionTimeoutMs = Math.clamp(config.getLong("storage.mysql.connection-timeout-ms", 8000L), 2_000L, 60_000L);
        String sslMode = config.getString("storage.mysql.ssl-mode", "REQUIRED");
        if (sslMode == null || sslMode.isBlank()) sslMode = "REQUIRED";
        sslMode = sslMode.trim().toUpperCase(Locale.ROOT);
        if (!switch (sslMode) {
            case "DISABLED", "PREFERRED", "REQUIRED", "VERIFY_CA", "VERIFY_IDENTITY" -> true;
            default -> false;
        }) throw new IllegalArgumentException("storage.mysql.ssl-mode must be one of DISABLED, PREFERRED, REQUIRED, VERIFY_CA or VERIFY_IDENTITY");

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?sslMode=" + sslMode + "&autoReconnect=false&characterEncoding=utf8");
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setMaximumPoolSize(poolSize);
        hikariConfig.setConnectionTimeout(connectionTimeoutMs);
        hikariConfig.setPoolName("PixelRPG-Hikari");
        hikariConfig.setMinimumIdle(Math.min(poolSize, Math.max(1, poolSize / 4)));
        hikariConfig.setIdleTimeout(300_000L);
        hikariConfig.setMaxLifetime(1_800_000L);
        hikariConfig.setKeepaliveTime(120_000L);
        hikariConfig.setValidationTimeout(5_000L);
        hikariConfig.setLeakDetectionThreshold(15_000L);
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        this.dataSource = new HikariDataSource(hikariConfig);
    }

    public void createTables() throws SQLException {
        String schemaSql = "CREATE TABLE IF NOT EXISTS pixelrpg_schema_version (version INT NOT NULL PRIMARY KEY)";
        String playersSql = """
                CREATE TABLE IF NOT EXISTS pixelrpg_players (
                    uuid CHAR(36) PRIMARY KEY,
                    registered BOOLEAN NOT NULL DEFAULT FALSE,
                    experience BIGINT NOT NULL DEFAULT 0,
                    money_minor_units BIGINT NOT NULL DEFAULT 0,
                    waypoints TEXT,
                    story_chapter INT NOT NULL DEFAULT -1,
                    completed_quests TEXT,
                    scoreboard_enabled BOOLEAN NOT NULL DEFAULT FALSE,
                    party_hud_enabled BOOLEAN NOT NULL DEFAULT FALSE,
                    quest_tracker_enabled BOOLEAN NOT NULL DEFAULT FALSE,
                    playtime_millis BIGINT NOT NULL DEFAULT 0,
                    persistence_revision BIGINT NOT NULL DEFAULT 0
                )
                """;
        String activeQuestsSql = "CREATE TABLE IF NOT EXISTS pixelrpg_active_quests (uuid CHAR(36) NOT NULL, quest_id VARCHAR(64) NOT NULL, amount INT NOT NULL DEFAULT 0, expiry BIGINT NOT NULL DEFAULT 0, PRIMARY KEY (uuid, quest_id))";
        String statsSql = "CREATE TABLE IF NOT EXISTS pixelrpg_player_stats (uuid CHAR(36) NOT NULL, stat_key VARCHAR(64) NOT NULL, value BIGINT NOT NULL DEFAULT 0, PRIMARY KEY (uuid, stat_key))";
        String equipmentSql = "CREATE TABLE IF NOT EXISTS pixelrpg_player_equipment (uuid CHAR(36) NOT NULL, slot VARCHAR(16) NOT NULL, item_yaml TEXT NOT NULL, PRIMARY KEY (uuid, slot))";

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate(schemaSql);
            statement.executeUpdate(playersSql);
            applyMigrations(connection);
            statement.executeUpdate(activeQuestsSql);
            statement.executeUpdate(statsSql);
            statement.executeUpdate(equipmentSql);
        }
    }

    private void applyMigrations(Connection connection) throws SQLException {
        int version = readSchemaVersion(connection);
        if (version < 1) {
            migrateLegacyPlayerColumns(connection);
            ensurePlayerRevisionColumn(connection);
            writeSchemaVersion(connection, 1);
            version = 1;
        }
        if (version < 2) {
            migrateMoneyToMinorUnits(connection);
            writeSchemaVersion(connection, 2);
            version = 2;
        }
        if (version != CURRENT_SCHEMA_VERSION) {
            throw new SQLException("Unsupported PixelRPG schema version: " + version + ", expected " + CURRENT_SCHEMA_VERSION);
        }
    }

    private int readSchemaVersion(Connection connection) throws SQLException {
        try (var statement = connection.prepareStatement("SELECT version FROM pixelrpg_schema_version ORDER BY version DESC LIMIT 1");
             var result = statement.executeQuery()) {
            return result.next() ? result.getInt(1) : 0;
        }
    }

    private void writeSchemaVersion(Connection connection, int version) throws SQLException {
        try (var statement = connection.prepareStatement("INSERT INTO pixelrpg_schema_version (version) VALUES (?)")) {
            statement.setInt(1, version);
            statement.executeUpdate();
        }
    }

    private void migrateMoneyToMinorUnits(Connection connection) throws SQLException {
        if (!hasColumn(connection, "pixelrpg_players", "money_minor_units")) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("ALTER TABLE pixelrpg_players ADD COLUMN money_minor_units BIGINT NOT NULL DEFAULT 0");
            }
        }
        if (hasColumn(connection, "pixelrpg_players", "money")) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("UPDATE pixelrpg_players SET money_minor_units = CASE WHEN money IS NULL OR money < 0 THEN 0 ELSE ROUND(money * 100) END");
                statement.executeUpdate("ALTER TABLE pixelrpg_players DROP COLUMN money");
            }
        }
    }

    private void ensurePlayerRevisionColumn(Connection connection) throws SQLException {
        if (hasColumn(connection, "pixelrpg_players", "persistence_revision")) return;
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE pixelrpg_players ADD COLUMN persistence_revision BIGINT NOT NULL DEFAULT 0");
        }
    }

    private void migrateLegacyPlayerColumns(Connection connection) throws SQLException {
        String[] legacyColumns = {"player_class", "start_bonus", "attr_vitality", "attr_agility", "attr_precision", "attr_range", "attr_toughness", "attr_soulview", "attr_elytra"};
        for (String column : legacyColumns) {
            if (!hasColumn(connection, "pixelrpg_players", column)) continue;
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("ALTER TABLE pixelrpg_players DROP COLUMN `" + column + "`");
            }
        }
    }

    private boolean hasColumn(Connection connection, String table, String column) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (var result = metadata.getColumns(connection.getCatalog(), null, table, column)) {
            return result.next();
        }
    }

    public DataSource getDataSource() { return dataSource; }
    public void shutdown() { if (dataSource != null && !dataSource.isClosed()) dataSource.close(); }

    private static String requireHost(String value) {
        String host = requireText(value, "storage.mysql.host");
        if (host.indexOf('/') >= 0 || host.indexOf('\\') >= 0 || host.indexOf('?') >= 0 || host.indexOf('#') >= 0 || host.indexOf('@') >= 0 || host.chars().anyMatch(Character::isWhitespace)) throw new IllegalArgumentException("storage.mysql.host contains invalid characters");
        return host;
    }

    private static String requireIdentifier(String value, String path) {
        String identifier = requireText(value, path);
        if (!identifier.matches("[A-Za-z0-9_$-]+")) throw new IllegalArgumentException(path + " contains invalid characters");
        return identifier;
    }

    private static String requireText(String value, String path) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(path + " must not be blank");
        return value.trim();
    }
}
