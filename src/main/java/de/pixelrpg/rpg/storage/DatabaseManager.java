// src/main/java/de/pixelrpg/rpg/storage/DatabaseManager.java (VOLLSTÄNDIG, ersetzt alte Datei — attr_elytra Spalte)
package de.pixelrpg.rpg.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseManager {

    private HikariDataSource dataSource;

    public void connect(FileConfiguration config) {
        String host = config.getString("storage.mysql.host", "localhost");
        int port = config.getInt("storage.mysql.port", 3306);
        String database = config.getString("storage.mysql.database", "pixelrpg");
        String username = config.getString("storage.mysql.username", "root");
        String password = config.getString("storage.mysql.password", "");
        int poolSize = config.getInt("storage.mysql.pool-size", 10);
        long connectionTimeoutMs = config.getLong("storage.mysql.connection-timeout-ms", 8000L);

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&autoReconnect=true&characterEncoding=utf8");
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setMaximumPoolSize(poolSize);
        hikariConfig.setConnectionTimeout(connectionTimeoutMs);
        hikariConfig.setPoolName("PixelRPG-Hikari");
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");

        this.dataSource = new HikariDataSource(hikariConfig);
    }

    public void createTables() throws SQLException {
        String playersSql = """
                CREATE TABLE IF NOT EXISTS pixelrpg_players (
                    uuid CHAR(36) PRIMARY KEY,
                    registered BOOLEAN NOT NULL DEFAULT FALSE,
                    experience BIGINT NOT NULL DEFAULT 0,
                    player_class VARCHAR(32) NOT NULL DEFAULT 'NONE',
                    money DOUBLE NOT NULL DEFAULT 0,
                    start_bonus BOOLEAN NOT NULL DEFAULT FALSE,
                    attr_vitality INT NOT NULL DEFAULT 0,
                    attr_agility INT NOT NULL DEFAULT 0,
                    attr_precision INT NOT NULL DEFAULT 0,
                    attr_range INT NOT NULL DEFAULT 0,
                    attr_toughness INT NOT NULL DEFAULT 0,
                    attr_soulview INT NOT NULL DEFAULT 0,
                    attr_elytra INT NOT NULL DEFAULT 0,
                    waypoints TEXT,
                    story_chapter INT NOT NULL DEFAULT -1,
                    completed_quests TEXT,
                    unlocked_achievements TEXT,
                    unlocked_titles TEXT,
                    selected_title VARCHAR(64),
                    scoreboard_enabled BOOLEAN NOT NULL DEFAULT TRUE,
                    party_hud_enabled BOOLEAN NOT NULL DEFAULT TRUE,
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

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(playersSql);
            statement.executeUpdate(activeQuestsSql);
            statement.executeUpdate(statsSql);
        }
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}