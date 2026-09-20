package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.npc.RPGNpc;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.Optional;

public record DialogueContext(Player player, RPGNpc npc, World world, Location location) {

    public DialogueContext {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(location, "location");
    }

    public static DialogueContext forPlayer(Player player) {
        Objects.requireNonNull(player, "player");
        return new DialogueContext(player, null, player.getWorld(), player.getLocation().clone());
    }

    public static DialogueContext forNpc(Player player, RPGNpc npc) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(npc, "npc");
        return new DialogueContext(player, npc, npc.location().getWorld(), npc.location().clone());
    }

    public Optional<RPGNpc> npcOptional() {
        return Optional.ofNullable(npc);
    }
}
