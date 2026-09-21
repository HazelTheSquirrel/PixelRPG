package de.pixelrpg.rpg.dialogue;

import org.bukkit.World;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;

import java.util.Objects;

public final class WorldStateListener implements Listener {
    private final WorldState worldState;
    private final PlayerKnowledgeStore knowledgeStore;

    public WorldStateListener(WorldState worldState, PlayerKnowledgeStore knowledgeStore) {
        this.worldState = Objects.requireNonNull(worldState, "worldState");
        this.knowledgeStore = Objects.requireNonNull(knowledgeStore, "knowledgeStore");
    }

    /** Records dimension entry as a persistent world and player discovery state. */
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        World world = event.getPlayer().getWorld();
        switch (world.getEnvironment()) {
            case NETHER -> {
                worldState.set("dimension.nether.entered");
                knowledgeStore.learn(event.getPlayer().getUniqueId(), "nether");
            }
            case THE_END -> {
                worldState.set("dimension.end.entered");
                knowledgeStore.learn(event.getPlayer().getUniqueId(), "end");
            }
            case NORMAL -> {
                worldState.set("dimension.overworld.entered");
                knowledgeStore.learn(event.getPlayer().getUniqueId(), "world.history");
            }
        }
    }

    /** Records major first-saga boss outcomes in the shared world state. */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof EnderDragon) {
            worldState.set("story.ender_dragon.defeated");
            worldState.set("saga.first.complete");
        } else if (entity instanceof Wither) {
            worldState.set("wither.defeated");
        }
    }

    /** Mirrors completed advancements into generic player knowledge and world state. */
    @EventHandler
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        String key = event.getAdvancement().getKey().toString();
        knowledgeStore.learn(event.getPlayer().getUniqueId(), "advancement:" + key);
        worldState.set("advancement:" + key);
    }
}
