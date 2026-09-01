package de.pixelrpg.rpg.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

public final class DatabaseManager {
    private HikariDataSource dataSource;

    public void connect(FileConfiguration config) {
        String host = requireText(config.getString("storage.mysql.host", "localhost"), "storage.mysql.host");
        int port = config.getInt("storage.mysql.port", 3306);
        if (port < 1 || port > 65535) throw new IllegalArgumentException("storage.mysql.port must be between 1 and 65535");
        String database = requireText(config.getString("storage.mysql.database", "pixelrpg"), "storage.mysql.database");
        String username = config.getString("storage.mysql.username", "root");
        String password = config.getString("storage.mysql.password", "");
        int poolSize = Math.max(2, config.getInt("storage.mysql.pool-size", 10));
        long connectionTimeoutMs = Math.max(2_000L, config.getLong("storage.mysql.connection-timeout-ms", 8000L));
        String sslMode = config.getString("storage.mysql.ssl-mode", "REQUIRED");
        if (sslMode == null || sslMode.isBlank()) sslMode = "REQUIRED";

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                + "?sslMode=" + sslMode.trim() + "&autoReconnect=false&characterEncoding=utf8");
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setMaximumPoolSize(poolSize);
        hikariConfig.setConnectionTimeout(connectionTimeoutMs);
        hikariConfig.setPoolName("PixelRPG-Hikari");
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikariConfig.setMinimumIdle(Math.min(poolSize, Math.max(1, poolSize / 4)));
        hikariConfig.setIdleTimeout(300_000L);
        hikariConfig.setMaxLifetime(1_800_000L);
        hikariConfig.setKeepaliveTime(120_000L);
        hikariConfig.setValidationTimeout(5_000L);
        hikariConfig.setLeakDetectionThreshold(15_000L);
        this.dataSource = new HikariDataSource(hikariConfig);
    }

    public void createTables() throws SQLException {
        String playersSql = """
                CREATE TABLE IF NOT EXISTS pixelrpg_players (
                    uuid CHAR(36) PRIMARY KEY,
                    registered BOOLEAN NOT NULL DEFAULT FALSE,
                    experience BIGINT NOT NULL DEFAULT 0,
                    money DOUBLE NOT NULL DEFAULT 0,
                    waypoints TEXT,
                    story_chapter INT NOT NULL DEFAULT -1,
                    completed_quests TEXT,
                    scoreboard_enabled BOOLEAN NOT NULL DEFAULT FALSE,
                    party_hud_enabled BOOLEAN NOT NULL DEFAULT FALSE,
                    quest_tracker_enabled BOOLEAN NOT NULL DEFAULT FALSE,
                    playtime_millis BIGINT NOT NULL DEFAULT 0
                )
                """;
        String activeQuestsSql = """
                CREATE TABLE IF NOT EXISTS pixelrpg_active_quests (
                    uuid CHAR(36) NOT NULL,
                    quest_id VARCHAR(64) NOT NULL,
                    amount INT NOT NULL DEFAULT 0,
                    expiry BIGINT NOT NULL DEFAULT 0,
                    PRIMARY KEY (uuid, quest_id)
                )
                """;
        String statsSql = """
                CREATE TABLE IF NOT EXISTS pixelrpg_player_stats (
                    uuid CHAR(36) NOT NULL,
                    stat_key VARCHAR(64) NOT NULL,
                    value BIGINT NOT NULL DEFAULT 0,
                    PRIMARY KEY (uuid, stat_key)
                )
                """;
        String equipmentSql = """
                CREATE TABLE IF NOT EXISTS pixelrpg_player_equipment (
                    uuid CHAR(36) NOT NULL,
                    slot VARCHAR(16) NOT NULL,
                    item_yaml TEXT NOT NULL,
                    PRIMARY KEY (uuid, slot)
                )
                """;
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate(playersSql);
            migrateLegacyPlayerColumns(connection);
            statement.executeUpdate(activeQuestsSql);
            statement.executeUpdate(statsSql);
            statement.executeUpdate(equipmentSql);
        }
    }

    private void migrateLegacyPlayerColumns(Connection connection) throws SQLException {
        String[] legacyColumns = {"player_class", "start_bonus", "attr_vitality", "attr_agility", "attr_precision", "attr_range", "attr_toughness", "attr_soulview", "attr_elytra"};
        DatabaseMetaData metadata = connection.getMetaData();
        for (String column : legacyColumns) {
            boolean exists;
            try (var result = metadata.getColumns(connection.getCatalog(), null, "pixelrpg_players", column)) {
                exists = result.next();
            }
            if (!exists) continue;
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("ALTER TABLE pixelrpg_players DROP COLUMN `" + column + "`");
            }
        }
    }

    public DataSource getDataSource() { return dataSource; }
    public void shutdown() { if (dataSource != null && !dataSource.isClosed()) dataSource.close(); }

    private static String requireText(String value, String path) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(path + " must not be blank");
        return value.trim();
    }
}
