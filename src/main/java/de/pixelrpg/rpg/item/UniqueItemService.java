package de.pixelrpg.rpg.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.pixelrpg.rpg.config.JsonDataManager;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class UniqueItemService implements AutoCloseable {
    private final Plugin plugin;
    private final Set<String> claimedDefinitions=new HashSet<>();
    private final ExecutorService ioExecutor;
    public UniqueItemService(Plugin plugin){
        this.plugin=plugin;
        this.ioExecutor=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"PixelRPG-UniqueItemIO");t.setDaemon(true);return t;});
        load();
    }
    public synchronized boolean claim(ItemDefinition definition){if(!definition.unique())return false;if(!claimedDefinitions.add(definition.id()))return false;persistAsync();return true;}
    public synchronized void release(ItemDefinition definition){if(!definition.unique()||!claimedDefinitions.remove(definition.id()))return;persistAsync();}
    public synchronized boolean isClaimed(ItemDefinition definition){return claimedDefinitions.contains(definition.id());}
    private void load(){JsonObject root=new JsonDataManager(plugin).load("unique-items.json");JsonArray claimed=root.getAsJsonArray("claimed");if(claimed!=null)for(var e:claimed)claimedDefinitions.add(e.getAsString());}
    private void persistAsync(){Set<String> snapshot;synchronized(this){snapshot=new HashSet<>(claimedDefinitions);}ioExecutor.execute(()->{JsonArray claimed=new JsonArray();snapshot.stream().sorted().forEach(claimed::add);JsonObject root=new JsonObject();root.add("claimed",claimed);new JsonDataManager(plugin).save("unique-items.json",root);});}
    @Override public void close(){ioExecutor.shutdown();}
}