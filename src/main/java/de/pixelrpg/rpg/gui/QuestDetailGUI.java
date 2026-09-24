package de.pixelrpg.rpg.gui;

import de.pixelrpg.rpg.item.ItemService;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.quest.Quest;
import de.pixelrpg.rpg.quest.QuestService;
import de.pixelrpg.rpg.quest.QuestType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Locale;

/** Detail view for one quest, including progress and the abandon action. */
public final class QuestDetailGUI extends AbstractGUI {
    private final Player viewer;
    private final QuestService quests;
    private final PlayerProfileManager profiles;
    private final Quest quest;
    private final ItemService itemService;

    public QuestDetailGUI(Player viewer, QuestService quests, PlayerProfileManager profiles, Quest quest, ItemService itemService) {
        super(54, Component.text(quest.title(), NamedTextColor.GOLD));
        this.viewer = viewer;
        this.quests = quests;
        this.profiles = profiles;
        this.quest = quest;
        this.itemService = itemService;
    }

    @Override
    protected void populate() {
        PlayerProfile profile = profiles.getProfile(viewer.getUniqueId()).orElse(null);
        if (profile == null) return;

        ItemStack info = new ItemStack(Material.WRITTEN_BOOK);
        ItemMeta meta = info.getItemMeta();
        meta.displayName(Component.text(quest.title(), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        meta.lore(java.util.List.of(
                Component.text(quest.description(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("Ziel: " + quest.targetKey(), NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false),
                Component.text("Benötigte Menge: " + quest.requiredAmount(), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false),
                Component.text("Benötigtes Level: " + quest.requiredLevel(), NamedTextColor.RED).decoration(TextDecoration.ITALIC, false),
                Component.text("Belohnung: " + quest.rewardMoney() + " Gold + " + quest.rewardExp() + " XP", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)
        ));
        info.setItemMeta(meta);
        setItem(22, info);

        if (quest.type() == QuestType.COLLECT) {
            ItemStack requested = requestedItem();
            if (requested != null) setItem(20, requested);
        }

        if (profile.hasActiveQuest(quest.id())) {
            var progress = profile.getActiveQuests().get(quest.id());
            ItemStack progressItem = new ItemStack(Material.EXPERIENCE_BOTTLE);
            ItemMeta progressMeta = progressItem.getItemMeta();
            progressMeta.displayName(Component.text("Fortschritt", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
            progressMeta.lore(java.util.List.of(Component.text(
                    progress.getCurrentAmount() + "/" + quest.requiredAmount(), NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)));
            progressItem.setItemMeta(progressMeta);
            setItem(24, progressItem);

            ItemStack abandon = new ItemStack(Material.BARRIER);
            ItemMeta abandonMeta = abandon.getItemMeta();
            abandonMeta.displayName(Component.text("Quest abbrechen", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            abandon.setItemMeta(abandonMeta);
            setItem(31, abandon, event -> {
                quests.abandon(viewer, quest.id());
                open(viewer);
            });
        }

        setItem(49, named(Material.ARROW, "Zurück", NamedTextColor.RED),
                event -> new QuestLogGUI(viewer, quests, profiles, itemService).open(viewer));
    }

    private ItemStack requestedItem() {
        String target = quest.targetKey();
        if (target == null || target.isBlank()) return null;
        try {
            Material material = Material.matchMaterial(target);
            if (material != null && material.isItem()) {
                return new ItemStack(material, Math.min(quest.requiredAmount(), material.getMaxStackSize()));
            }
        } catch (IllegalArgumentException ignored) { }
        if (itemService == null) return null;
        if (target.toLowerCase(Locale.ROOT).startsWith("pixelrpg:")) {
            return itemService.createItem(target).map(item -> {
                item.setAmount(Math.min(quest.requiredAmount(), item.getMaxStackSize()));
                return item;
            }).orElse(null);
        }
        return null;
    }

    private ItemStack named(Material material, String name, NamedTextColor color) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }
}
