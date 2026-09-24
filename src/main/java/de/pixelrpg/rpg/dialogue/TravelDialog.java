package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.npc.NpcManager;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/** Native travel dialog with safe NPC arrival-position resolution. */
public final class TravelDialog {
    private static final int PREFERRED_DISTANCE = 3;
    private static final int SECONDARY_DISTANCE = 4;
    private static final int MAX_SEARCH_DISTANCE = 8;

    private final NpcManager npcManager;
    private final PlayerProfileManager profileManager;
    private final DialogueEngine dialogueEngine;

    public TravelDialog(NpcManager npcManager, PlayerProfileManager profileManager, DialogueEngine dialogueEngine) {
        this.npcManager = npcManager;
        this.profileManager = profileManager;
        this.dialogueEngine = dialogueEngine;
    }

    public void open(Player player, String currentNpcId) {
        open(player, currentNpcId, Player::closeDialog);
    }

    public void open(Player player, String currentNpcId, Consumer<Player> backAction) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) {
            dialogueEngine.openUnavailable(player, "Reisen", "Dein Spielerprofil konnte nicht geladen werden.");
            return;
        }

        List<RPGNpc> destinations = npcManager.getAll().stream()
                .filter(npc -> npc.type() == NpcType.TRAVEL)
                .filter(npc -> currentNpcId == null || !currentNpcId.equals(npc.id()))
                .filter(npc -> profile.hasUnlockedWaypoint(npc.id()))
                .toList();

        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text("Wähle einen freigeschalteten Reisepunkt.", NamedTextColor.WHITE)),
                DialogBody.plainMessage(Component.text(
                        "Nicht entdeckte Orte werden automatisch freigeschaltet, sobald du sie besuchst.",
                        NamedTextColor.WHITE))
        );

        if (destinations.isEmpty()) {
            dialogueEngine.openMultiAction(
                    player,
                    Component.text("Reisen", NamedTextColor.GOLD),
                    List.of(DialogBody.plainMessage(Component.text(
                            "Du hast noch keinen weiteren Reisepunkt freigeschaltet.",
                            NamedTextColor.WHITE))),
                    List.of(dialogueEngine.actionButton(Component.text("Zurück"), NamedTextColor.WHITE, backAction)),
                    1);
            return;
        }

        List<ActionButton> actions = new ArrayList<>();
        for (RPGNpc destination : destinations) {
            actions.add(dialogueEngine.actionButton(
                    Component.text(destination.name()),
                    NamedTextColor.GREEN,
                    target -> teleport(target, destination)));
        }

        dialogueEngine.openMultiAction(
                player,
                Component.text("Reisen", NamedTextColor.GOLD),
                body,
                actions,
                2);
    }

    private void teleport(Player player, RPGNpc destination) {
        Location arrival = findSafeArrivalLocation(destination.location());
        if (arrival == null) {
            player.sendMessage(Component.text("Für diesen Reisepunkt wurde keine sichere Ankunftsposition gefunden.", NamedTextColor.RED));
            return;
        }

        player.teleportAsync(arrival).thenAccept(success -> {
            if (!success) return;
            Bukkit.getScheduler().runTask(PixelRPGPlugin.getInstance(), () -> {
                player.closeDialog();
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            });
        });
    }

    private Location findSafeArrivalLocation(Location npcLocation) {
        World world = npcLocation.getWorld();
        if (world == null) return null;

        List<Location> candidates = new ArrayList<>();
        for (int distance : List.of(PREFERRED_DISTANCE, SECONDARY_DISTANCE)) {
            addRingCandidates(candidates, npcLocation, distance);
        }

        for (int distance = 5; distance <= MAX_SEARCH_DISTANCE; distance++) {
            addRingCandidates(candidates, npcLocation, distance);
        }

        return candidates.stream()
                .map(location -> findSafeGroundLocation(world, location))
                .filter(location -> location != null)
                .min(Comparator.comparingDouble(location -> location.distanceSquared(npcLocation)))
                .orElse(null);
    }

    private void addRingCandidates(List<Location> candidates, Location center, int distance) {
        int samples = Math.max(16, distance * 12);
        for (int index = 0; index < samples; index++) {
            double angle = (Math.PI * 2.0 * index) / samples;
            double x = center.getX() + Math.cos(angle) * distance;
            double z = center.getZ() + Math.sin(angle) * distance;
            candidates.add(new Location(center.getWorld(), x, center.getY(), z));
        }
    }

    private Location findSafeGroundLocation(World world, Location horizontal) {
        int centerY = horizontal.getBlockY();
        int minY = Math.max(world.getMinHeight(), centerY - 6);
        int maxY = Math.min(world.getMaxHeight() - 2, centerY + 6);

        for (int y = centerY; y >= minY; y--) {
            Location candidate = new Location(world, horizontal.getX(), y, horizontal.getZ());
            if (isSafeGround(candidate)) return candidate;
        }

        for (int y = centerY + 1; y <= maxY; y++) {
            Location candidate = new Location(world, horizontal.getX(), y, horizontal.getZ());
            if (isSafeGround(candidate)) return candidate;
        }

        return null;
    }

    private boolean isSafeGround(Location location) {
        World world = location.getWorld();
        if (world == null || location.getBlockY() <= world.getMinHeight()) return false;

        Block ground = world.getBlockAt(location.getBlockX(), location.getBlockY() - 1, location.getBlockZ());
        Block feet = world.getBlockAt(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        Block head = world.getBlockAt(location.getBlockX(), location.getBlockY() + 1, location.getBlockZ());

        if (!ground.getType().isSolid()) return false;
        if (!feet.isPassable() || !head.isPassable()) return false;
        if (isDangerous(ground.getType()) || isDangerous(feet.getType()) || isDangerous(head.getType())) return false;

        return location.getBlockY() > world.getMinHeight() + 1;
    }

    private boolean isDangerous(Material material) {
        return switch (material) {
            case LAVA, FIRE, SOUL_FIRE, CAMPFIRE, SOUL_CAMPFIRE, MAGMA_BLOCK,
                 CACTUS, SWEET_BERRY_BUSH, POWDER_SNOW, POINTED_DRIPSTONE, WITHER_ROSE -> true;
            default -> false;
        };
    }
}
