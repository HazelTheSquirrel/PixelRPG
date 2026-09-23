package de.pixelrpg.rpg.profession;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import org.bukkit.plugin.Plugin;
import java.util.Objects;
import java.util.concurrent.*;
/** Owns profession definitions, crafting data, activity listeners and their asynchronous lifecycle. */
public final class ProfessionSystem implements AutoCloseable{
 private final Plugin plugin;private final PlayerProfileManager profiles;private final ProfessionService professionService;private final CraftingRecipeRegistry registry=new CraftingRecipeRegistry();private final ExecutorService io;private volatile CraftingService craftingService;
 public ProfessionSystem(Plugin plugin,PlayerProfileManager profiles){this.plugin=Objects.requireNonNull(plugin);this.profiles=Objects.requireNonNull(profiles);professionService=new ProfessionService(profiles);io=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"PixelRPG-ProfessionIO");t.setDaemon(true);return t;});}
 public CompletableFuture<Void> loadAsync(){return CompletableFuture.runAsync(()->registry.load(plugin),io);}
 public void register(){plugin.getServer().getPluginManager().registerEvents(new ProfessionActivityListener(professionService),plugin);}
 public ProfessionService professionService(){return professionService;}
 public CraftingService craftingService(){CraftingService current=craftingService;if(current==null){current=new CraftingService(professionService,profiles,registry);craftingService=current;}return current;}
 public CraftingRecipeRegistry craftingRecipeRegistry(){return registry;}
 @Override public void close(){io.shutdown();try{if(!io.awaitTermination(10,TimeUnit.SECONDS))io.shutdownNow();}catch(InterruptedException e){io.shutdownNow();Thread.currentThread().interrupt();}}
}
