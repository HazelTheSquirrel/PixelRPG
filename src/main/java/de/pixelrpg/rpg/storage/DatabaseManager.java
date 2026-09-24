package de.pixelrpg.rpg.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;
import javax.sql.DataSource;
import java.sql.*;
import java.util.Locale;
import java.util.Set;

public final class DatabaseManager implements AutoCloseable {
    private static final int CURRENT_SCHEMA_VERSION = 2;
    private HikariDataSource dataSource;

    public void connect(FileConfiguration config) {
        String host = requireHost(config.getString("storage.mysql.host", "localhost"));
        int port = Math.clamp(config.getInt("storage.mysql.port", 3306), 1, 65535);
        String database = requireIdentifier(config.getString("storage.mysql.database", "pixelrpg"), "storage.mysql.database");
        String username = config.getString("storage.mysql.username", "root");
        String password = config.getString("storage.mysql.password", "");
        int poolSize = Math.clamp(config.getInt("storage.mysql.pool-size", 10), 2, 64);
        long timeout = Math.clamp(config.getLong("storage.mysql.connection-timeout-ms", 8000L), 2000L, 60000L);
        String sslMode = config.getString("storage.mysql.ssl-mode", "REQUIRED");
        if (sslMode == null || sslMode.isBlank()) sslMode = "REQUIRED";
        sslMode = sslMode.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("DISABLED","PREFERRED","REQUIRED","VERIFY_CA","VERIFY_IDENTITY").contains(sslMode)) {
            throw new IllegalArgumentException("storage.mysql.ssl-mode is invalid");
        }

        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?sslMode=" + sslMode + "&autoReconnect=false&characterEncoding=utf8");
        hikari.setUsername(username);
        hikari.setPassword(password);
        hikari.setMaximumPoolSize(poolSize);
        hikari.setMinimumIdle(Math.min(poolSize, Math.max(1, poolSize / 4)));
        hikari.setConnectionTimeout(timeout);
        hikari.setPoolName("PixelRPG-Hikari");
        hikari.setIdleTimeout(300_000L);
        hikari.setMaxLifetime(1_800_000L);
        hikari.setKeepaliveTime(120_000L);
        hikari.setValidationTimeout(5_000L);
        hikari.setLeakDetectionThreshold(15_000L);
        hikari.addDataSourceProperty("cachePrepStmts", "true");
        hikari.addDataSourceProperty("prepStmtCacheSize", "250");
        hikari.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikari.addDataSourceProperty("useServerPrepStmts", "true");
        dataSource = new HikariDataSource(hikari);
    }

    public void createSchema() throws SQLException {
        String schema = "CREATE TABLE IF NOT EXISTS pixelrpg_schema_version (version INT NOT NULL PRIMARY KEY)";
        String players = """
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
        String quests = "CREATE TABLE IF NOT EXISTS pixelrpg_active_quests (uuid CHAR(36) NOT NULL, quest_id VARCHAR(64) NOT NULL, amount INT NOT NULL DEFAULT 0, expiry BIGINT NOT NULL DEFAULT 0, PRIMARY KEY(uuid,quest_id))";
        String stats = "CREATE TABLE IF NOT EXISTS pixelrpg_player_stats (uuid CHAR(36) NOT NULL, stat_key VARCHAR(64) NOT NULL, value BIGINT NOT NULL DEFAULT 0, PRIMARY KEY(uuid,stat_key))";
        String equipment = "CREATE TABLE IF NOT EXISTS pixelrpg_player_equipment (uuid CHAR(36) NOT NULL, slot VARCHAR(16) NOT NULL, item_yaml TEXT NOT NULL, PRIMARY KEY(uuid,slot))";
        try (Connection c=dataSource.getConnection(); Statement s=c.createStatement()) {
            s.executeUpdate(schema);
            s.executeUpdate(players);
            migrate(c);
            s.executeUpdate(quests);
            s.executeUpdate(stats);
            s.executeUpdate(equipment);
        }
    }

    private void migrate(Connection c) throws SQLException {
        int version = readVersion(c);
        if (version < 1) {
            dropLegacyColumns(c);
            ensureColumn(c, "pixelrpg_players", "persistence_revision", "BIGINT NOT NULL DEFAULT 0");
            writeVersion(c, 1);
            version = 1;
        }
        if (version < 2) {
            ensureColumn(c, "pixelrpg_players", "money_minor_units", "BIGINT NOT NULL DEFAULT 0");
            if (hasColumn(c, "pixelrpg_players", "money")) {
                try (Statement s=c.createStatement()) {
                    s.executeUpdate("UPDATE pixelrpg_players SET money_minor_units = CASE WHEN money IS NULL OR money < 0 THEN 0 ELSE ROUND(money * 100) END");
                    s.executeUpdate("ALTER TABLE pixelrpg_players DROP COLUMN money");
                }
            }
            writeVersion(c, 2);
            version = 2;
        }
        if (version != CURRENT_SCHEMA_VERSION) throw new SQLException("Unsupported PixelRPG schema version: " + version);
    }

    private int readVersion(Connection c) throws SQLException {
        try (PreparedStatement p=c.prepareStatement("SELECT version FROM pixelrpg_schema_version ORDER BY version DESC LIMIT 1"); ResultSet r=p.executeQuery()) {
            return r.next() ? r.getInt(1) : 0;
        }
    }

    private void writeVersion(Connection c,int version) throws SQLException {
        try (PreparedStatement p=c.prepareStatement("INSERT INTO pixelrpg_schema_version(version) VALUES(?)")) { p.setInt(1,version); p.executeUpdate(); }
    }

    private void dropLegacyColumns(Connection c) throws SQLException {
        String[] columns={"player_class","start_bonus","attr_vitality","attr_agility","attr_precision","attr_range","attr_toughness","attr_soulview","attr_elytra"};
        for(String column:columns) if(hasColumn(c,"pixelrpg_players",column)) try(Statement s=c.createStatement()){s.executeUpdate("ALTER TABLE pixelrpg_players DROP COLUMN `"+column+"`");}
    }

    private void ensureColumn(Connection c,String table,String column,String definition) throws SQLException {
        if(hasColumn(c,table,column)) return;
        try(Statement s=c.createStatement()){s.executeUpdate("ALTER TABLE "+table+" ADD COLUMN "+column+" "+definition);}
    }

    private boolean hasColumn(Connection c,String table,String column) throws SQLException {
        try(ResultSet r=c.getMetaData().getColumns(c.getCatalog(),null,table,column)){return r.next();}
    }

    public DataSource dataSource(){if(dataSource==null)throw new IllegalStateException("Database not connected.");return dataSource;}
    @Override public void close(){if(dataSource!=null&&!dataSource.isClosed())dataSource.close();}

    private static String requireHost(String value){String v=require(value,"storage.mysql.host");if(v.indexOf('/')>=0||v.indexOf('\\')>=0||v.indexOf('?')>=0||v.indexOf('#')>=0||v.indexOf('@')>=0||v.chars().anyMatch(Character::isWhitespace))throw new IllegalArgumentException("storage.mysql.host contains invalid characters");return v;}
    private static String requireIdentifier(String value,String path){String v=require(value,path);if(!v.matches("[A-Za-z0-9_$-]+"))throw new IllegalArgumentException(path+" contains invalid characters");return v;}
    private static String require(String value,String path){if(value==null||value.isBlank())throw new IllegalArgumentException(path+" must not be blank");return value.trim();}
}
