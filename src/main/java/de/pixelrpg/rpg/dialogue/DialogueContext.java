package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.npc.RPGNpc;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.Optional;

public record DialogueContext(
        Player player,
        RPGNpc npc,
        World world,
        Location location,
        DialogueDomainState domainState
) {
    public DialogueContext(Player player, RPGNpc npc, World world, Location location) {
        this(player, npc, world, location, null);
    }

    public DialogueContext {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(location, "location");
    }

    public static DialogueContext forPlayer(Player player) {
        Objects.requireNonNull(player, "player");
        return new DialogueContext(player, null, player.getWorld(), player.getLocation().clone(), null);
    }

    public static DialogueContext forNpc(Player player, RPGNpc npc) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(npc, "npc");
        return new DialogueContext(player, npc, npc.location().getWorld(), npc.location().clone(), null);
    }

    public static DialogueContext forNpc(Player player, RPGNpc npc, DialogueDomainState domainState) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(npc, "npc");
        return new DialogueContext(player, npc, npc.location().getWorld(), npc.location().clone(), domainState);
    }

    public Optional<RPGNpc> npcOptional() {
        return Optional.ofNullable(npc);
    }

    public Optional<DialogueDomainState> domainStateOptional() {
        return Optional.ofNullable(domainState);
    }
}
