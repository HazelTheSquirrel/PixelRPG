package de.pixelrpg.rpg.companion;

import de.pixelrpg.rpg.dialogue.DialogueEngine;
import de.pixelrpg.rpg.npc.NpcRuntimeManager;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Lifecycle owner for companion definition I/O and runtime listeners. */
public final class CompanionSystem implements AutoCloseable {
    private final Plugin plugin;
    private final PlayerProfileManager profiles;
    private final NpcRuntimeManager npcs;
    private final ExecutorService io=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"PixelRPG-CompanionIO");t.setDaemon(true);return t;});
    private CompanionService service;

    public CompanionSystem(Plugin plugin,PlayerProfileManager profiles,NpcRuntimeManager npcs){this.plugin=Objects.requireNonNull(plugin);this.profiles=Objects.requireNonNull(profiles);this.npcs=Objects.requireNonNull(npcs);}

    public CompletableFuture<CompanionService> loadAsync(){
        return CompletableFuture.supplyAsync(()->new CompanionRegistryLoader().load(),io).thenApply(registry->{
            service=new CompanionService(plugin,profiles,registry);
            return service;
        });
    }

    public CompanionService service(){if(service==null)throw new IllegalStateException("Companion system not loaded");return service;}
    public void register(){plugin.getServer().getPluginManager().registerEvents(service().runtimeListener(),plugin);plugin.getServer().getPluginManager().registerEvents(new CompanionExperienceListener(plugin,service()),plugin);plugin.getServer().getPluginManager().registerEvents(new CompanionBossRewardListener(plugin,service()),plugin);}
    @Override public void close(){if(service!=null)service.shutdown();io.shutdown();}

    private final class CompanionRegistryLoader {
        CompanionRegistry load(){return CompanionRegistry.load(plugin);}
    }
}
