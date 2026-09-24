package de.pixelrpg.rpg.player;
import org.bukkit.configuration.file.YamlConfiguration;
import java.nio.file.*;
import java.util.UUID;
import java.util.concurrent.*;
public final class YamlPlayerProfileRepository implements PlayerProfileRepository {
 private final Path dir; private final Executor io; public YamlPlayerProfileRepository(Path d,Executor e){dir=d;io=e;}
 public CompletableFuture<PlayerProfile> load(UUID id){return CompletableFuture.supplyAsync(()->{try{Files.createDirectories(dir);Path f=dir.resolve(id+".yml");PlayerProfile p=new PlayerProfile(id);if(!Files.exists(f))return p;var y=YamlConfiguration.loadConfiguration(f.toFile());p.registered(y.getBoolean("registered"));p.experience(y.getLong("experience"));p.moneyMinorUnits(y.getLong("money-minor-units"));p.storyChapter(y.getInt("story-chapter",-1));p.revision(y.getLong("revision"));return p;}catch(Exception e){throw new CompletionException(e);}},io);}
 public CompletableFuture<Void> save(PlayerProfile p){return CompletableFuture.runAsync(()->{try{Files.createDirectories(dir);var y=new YamlConfiguration();y.set("registered",p.registered());y.set("experience",p.experience());y.set("money-minor-units",p.moneyMinorUnits());y.set("story-chapter",p.storyChapter());y.set("revision",p.revision());y.save(dir.resolve(p.uniqueId()+".yml").toFile());}catch(Exception e){throw new CompletionException(e);}},io);}
}
