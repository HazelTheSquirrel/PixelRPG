package de.pixelrpg.rpg.player;
import de.pixelrpg.rpg.storage.DatabaseManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import java.util.*;
import java.util.concurrent.*;
public final class PlayerProfileManager implements AutoCloseable {
 private final Plugin plugin; private final Map<UUID,PlayerProfile> profiles=new ConcurrentHashMap<>(); private ExecutorService io; private PlayerProfileRepository repository; private DatabaseManager database;
 public PlayerProfileManager(Plugin p){plugin=p;}
 public void initialize(FileConfiguration c){io=Executors.newVirtualThreadPerTaskExecutor();if("MYSQL".equalsIgnoreCase(c.getString("storage.type","YAML"))){database=new DatabaseManager();database.connect(c);try{database.createSchema();}catch(Exception e){database.close();throw new IllegalStateException("Database initialization failed.",e);}repository=new MySQLPlayerProfileRepository(database.dataSource(),io);}else repository=new YamlPlayerProfileRepository(plugin.getDataFolder().toPath().resolve("players"),io);}
 public CompletableFuture<PlayerProfile> load(UUID id){return repository.load(id).thenApply(p->{profiles.put(id,p);return p;});}
 public PlayerProfile get(UUID id){return profiles.get(id);}
 public CompletableFuture<Void> save(UUID id){var p=profiles.get(id);if(p==null)return CompletableFuture.completedFuture(null);p.revision(p.revision()+1);return repository.save(p);}
 public CompletableFuture<Void> saveAll(){return CompletableFuture.allOf(profiles.keySet().stream().map(this::save).toArray(CompletableFuture[]::new));}
 public void unload(UUID id){profiles.remove(id);}
 @Override public void close(){try{saveAll().join();}finally{if(repository!=null)repository.close();if(database!=null)database.close();if(io!=null)io.close();profiles.clear();}}
}
