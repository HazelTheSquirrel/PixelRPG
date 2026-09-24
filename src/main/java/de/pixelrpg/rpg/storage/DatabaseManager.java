package de.pixelrpg.rpg.storage;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;
import javax.sql.DataSource;
import java.sql.SQLException;
public final class DatabaseManager implements AutoCloseable {
 private HikariDataSource dataSource;
 public void connect(FileConfiguration c){
  String host=require(c.getString("storage.mysql.host","localhost"),"storage.mysql.host");
  int port=Math.clamp(c.getInt("storage.mysql.port",3306),1,65535);
  String db=identifier(c.getString("storage.mysql.database","pixelrpg"),"storage.mysql.database");
  HikariConfig h=new HikariConfig();
  h.setJdbcUrl("jdbc:mysql://"+host+":"+port+"/"+db+"?sslMode="+require(c.getString("storage.mysql.ssl-mode","REQUIRED"),"storage.mysql.ssl-mode"));
  h.setUsername(c.getString("storage.mysql.username","root")); h.setPassword(c.getString("storage.mysql.password",""));
  h.setMaximumPoolSize(Math.clamp(c.getInt("storage.mysql.pool-size",10),2,64)); h.setPoolName("PixelRPG-Hikari");
  dataSource=new HikariDataSource(h);
 }
 public void createSchema() throws SQLException {
  try(var c=dataSource.getConnection();var s=c.createStatement()){
   s.executeUpdate("CREATE TABLE IF NOT EXISTS pixelrpg_players (uuid CHAR(36) PRIMARY KEY, registered BOOLEAN NOT NULL DEFAULT FALSE, experience BIGINT NOT NULL DEFAULT 0, money_minor_units BIGINT NOT NULL DEFAULT 0, story_chapter INT NOT NULL DEFAULT -1, persistence_revision BIGINT NOT NULL DEFAULT 0)");
   s.executeUpdate("CREATE TABLE IF NOT EXISTS pixelrpg_player_stats (uuid CHAR(36) NOT NULL, stat_key VARCHAR(64) NOT NULL, value BIGINT NOT NULL DEFAULT 0, PRIMARY KEY(uuid,stat_key))");
  }
 }
 public DataSource dataSource(){if(dataSource==null)throw new IllegalStateException("Database not connected.");return dataSource;}
 @Override public void close(){if(dataSource!=null)dataSource.close();}
 private static String require(String v,String p){if(v==null||v.isBlank())throw new IllegalArgumentException(p+" must not be blank.");return v.trim();}
 private static String identifier(String v,String p){String x=require(v,p);if(!x.matches("[A-Za-z0-9_$-]+"))throw new IllegalArgumentException(p+" contains invalid characters.");return x;}
}
