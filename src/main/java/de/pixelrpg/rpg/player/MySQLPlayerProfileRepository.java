package de.pixelrpg.rpg.player;
import javax.sql.DataSource;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.*;
public final class MySQLPlayerProfileRepository implements PlayerProfileRepository {
 private final DataSource ds; private final Executor io; public MySQLPlayerProfileRepository(DataSource d,Executor e){ds=d;io=e;}
 public CompletableFuture<PlayerProfile> load(UUID id){return CompletableFuture.supplyAsync(()->{try(var c=ds.getConnection();var s=c.prepareStatement("SELECT registered,experience,money_minor_units,story_chapter,persistence_revision FROM pixelrpg_players WHERE uuid=?")){s.setString(1,id.toString());try(var r=s.executeQuery()){var p=new PlayerProfile(id);if(r.next()){p.registered(r.getBoolean(1));p.experience(r.getLong(2));p.moneyMinorUnits(r.getLong(3));p.storyChapter(r.getInt(4));p.revision(r.getLong(5));}return p;}}catch(SQLException e){throw new CompletionException(e);}},io);}
 public CompletableFuture<Void> save(PlayerProfile p){return CompletableFuture.runAsync(()->{try(var c=ds.getConnection();var s=c.prepareStatement("INSERT INTO pixelrpg_players(uuid,registered,experience,money_minor_units,story_chapter,persistence_revision) VALUES(?,?,?,?,?,?) ON DUPLICATE KEY UPDATE registered=VALUES(registered),experience=VALUES(experience),money_minor_units=VALUES(money_minor_units),story_chapter=VALUES(story_chapter),persistence_revision=VALUES(persistence_revision)")){s.setString(1,p.uniqueId().toString());s.setBoolean(2,p.registered());s.setLong(3,p.experience());s.setLong(4,p.moneyMinorUnits());s.setInt(5,p.storyChapter());s.setLong(6,p.revision());s.executeUpdate();}catch(SQLException e){throw new CompletionException(e);}},io);}
}
