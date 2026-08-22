package de.pixelrpg.rpg.quest;

import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.npc.NpcManager;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Maintains one Recovery Compass as the player's current quest direction helper. */
public final class QuestNavigationService {
    private final Plugin plugin;
    private final QuestRepository questRepository;
    private final PlayerProfileManager profileManager;
    private final NpcManager npcManager;
    private final Map<UUID, String> currentTargets = new HashMap<>();

    public QuestNavigationService(Plugin plugin, QuestRepository questRepository,
                                  PlayerProfileManager profileManager, NpcManager npcManager) {
        this.plugin = plugin;
        this.questRepository = questRepository;
        this.profileManager = profileManager;
        this.npcManager = npcManager;
    }

    /** Refreshes the single navigable quest target for one player. */
    public void refresh(Player player) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegisteredInGuild()) {
            removeCompass(player);
            return;
        }

        QuestTarget target = findFirstNavigableTarget(profile);
        if (target == null) {
            currentTargets.remove(player.getUniqueId());
            removeCompass(player);
            return;
        }

        String targetKey = target.quest().id() + "@" + locationKey(target.location());
        if (!targetKey.equals(currentTargets.get(player.getUniqueId()))) {
            currentTargets.put(player.getUniqueId(), targetKey);
            player.sendActionBar(Component.text("Navigation: ", NamedTextColor.WHITE)
                    .append(Component.text(target.quest().title(), NamedTextColor.YELLOW)));
        }
        updateCompass(player, target.quest(), target.location());
    }

    public void clear(Player player) {
        currentTargets.remove(player.getUniqueId());
        removeCompass(player);
    }

    private QuestTarget findFirstNavigableTarget(PlayerProfile profile) {
        for (QuestProgress progress : profile.getActiveQuests().values()) {
            Quest quest = questRepository.getQuest(progress.getQuestId());
            if (quest == null || progress.getCurrentAmount() >= quest.requiredAmount()) continue;

            Location location = switch (quest.type()) {
                case REACH_LOCATION -> quest.reachLocation();
                case ESCORT -> quest.escortDestination();
                case TALK_TO_NPC -> npcManager.getById(quest.targetKey()).map(npc -> npc.location()).orElse(null);
                default -> null;
            };
            if (location != null && location.getWorld() != null) return new QuestTarget(quest, location);
        }
        return null;
    }

    private void updateCompass(Player player, Quest quest, Location target) {
        ItemStack compass = findCompass(player);
        if (compass == null) {
            compass = new ItemStack(Material.RECOVERY_COMPASS);
            player.getInventory().addItem(compass);
            compass = findCompass(player);
            if (compass == null) return;
        }

        ItemMeta rawMeta = compass.getItemMeta();
        if (!(rawMeta instanceof CompassMeta meta)) return;
        meta.setLodestone(target.clone());
        meta.setLodestoneTracked(false);
        meta.displayName(Component.text("Quest-Navigation", NamedTextColor.WHITE));
        meta.lore(java.util.List.of(
                Component.text(quest.title(), NamedTextColor.YELLOW),
                Component.text("Richtung zum aktuellen Questziel", NamedTextColor.WHITE)
        ));
        meta.getPersistentDataContainer().set(RPGKeys.Quest.navigationCompass(), PersistentDataType.BYTE, (byte) 1);
        compass.setItemMeta(meta);
    }

    private ItemStack findCompass(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() != Material.RECOVERY_COMPASS) continue;
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.getPersistentDataContainer().has(RPGKeys.Quest.navigationCompass(), PersistentDataType.BYTE)) {
                return item;
            }
        }
        return null;
    }

    private void removeCompass(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() != Material.RECOVERY_COMPASS) continue;
            ItemMeta meta = item.getItemMeta();
            if (meta == null || !meta.getPersistentDataContainer().has(RPGKeys.Quest.navigationCompass(), PersistentDataType.BYTE)) continue;
            item.setAmount(0);
        }
    }

    private String locationKey(Location location) {
        return location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    private record QuestTarget(Quest quest, Location location) {
    }
}
