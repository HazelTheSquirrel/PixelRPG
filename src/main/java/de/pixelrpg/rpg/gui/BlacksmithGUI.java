package de.pixelrpg.rpg.gui;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.combat.gem.GemItemFactory;
import de.pixelrpg.rpg.combat.gem.GemSocketService;
import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.item.ItemEconomyConfig;
import de.pixelrpg.rpg.item.ItemRarity;
import de.pixelrpg.rpg.item.RPGItemBuilder;
import de.pixelrpg.rpg.item.SoulboundService;
import de.pixelrpg.rpg.lang.LanguageManager;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public final class BlacksmithGUI implements Listener {

    private static final int ITEM_SLOT = 13;
    private static final int IDENTIFY_BUTTON = 11;
    private static final int SOULBOUND_BUTTON = 15;

    private final PlayerProfileManager profileManager;
    private final ItemEconomyConfig economyConfig;
    private final LanguageManager lang;

    public static final class BlacksmithHolder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    public BlacksmithGUI(PlayerProfileManager profileManager, ItemEconomyConfig economyConfig) {
        this.profileManager = profileManager;
        this.economyConfig = economyConfig;
        this.lang = PixelRPGPlugin.getInstance().getLanguageManager();
    }

    public void open(Player player) {
        BlacksmithHolder holder = new BlacksmithHolder();
        Inventory inventory = Bukkit.createInventory(holder, 27,
                lang.get("blacksmith.gui-title").color(NamedTextColor.DARK_GRAY));
        holder.inventory = inventory;

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.text(" "));
        filler.setItemMeta(fillerMeta);

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (slot != ITEM_SLOT && slot != IDENTIFY_BUTTON && slot != SOULBOUND_BUTTON) {
                inventory.setItem(slot, filler);
            }
        }

        inventory.setItem(IDENTIFY_BUTTON,
                buildButton(Material.ANVIL, "blacksmith.identify-button", NamedTextColor.YELLOW, "blacksmith.identify-desc"));
        inventory.setItem(SOULBOUND_BUTTON, buildSoulbindButton());
        player.openInventory(inventory);
    }

    private ItemStack buildButton(Material material, String nameKey, NamedTextColor color, String descriptionKey) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(lang.get(nameKey).color(color).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(lang.get(descriptionKey).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildSoulbindButton() {
        ItemStack item = new ItemStack(Material.SOUL_SAND);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(lang.get("blacksmith.soulbind-button").color(NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                lang.get("blacksmith.soulbind-desc").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("Benötigtes Level: " + economyConfig.getSoulboundMinLevel(), NamedTextColor.GOLD)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    // Verhindert das Verschieben von GUI-Dekorationen und Schaltflächen per Drag.
    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof BlacksmithHolder)) return;
        for (int slot : event.getRawSlots()) {
            if (slot < event.getView().getTopInventory().getSize() && slot != ITEM_SLOT) {
                event.setCancelled(true);
                return;
            }
        }
    }

    // Verarbeitet Identifizieren, Seelenbindung und das Einsetzen von Skill-Gems im Schmiedemenü.
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof BlacksmithHolder holder)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory top = holder.getInventory();
        if (event.getClick().isShiftClick()) {
            event.setCancelled(true);
            return;
        }
        if (event.getClickedInventory() != top) return;

        int slot = event.getSlot();
        if (slot == IDENTIFY_BUTTON) {
            event.setCancelled(true);
            processIdentify(player, top);
            return;
        }
        if (slot == SOULBOUND_BUTTON) {
            event.setCancelled(true);
            processSoulbound(player, top);
            return;
        }
        if (slot != ITEM_SLOT) {
            event.setCancelled(true);
            return;
        }

        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();
        if (cursor == null || cursor.getType() == Material.AIR || current == null || current.getType() == Material.AIR) return;
        if (event.getAction() == org.bukkit.event.inventory.InventoryAction.SWAP_WITH_CURSOR) {
            handleGemSocket(event, player, cursor, current);
        }
    }

    private void handleGemSocket(InventoryClickEvent event, Player player, ItemStack gem, ItemStack item) {
        String gemId = GemItemFactory.readGemId(gem);
        if (gemId == null || !item.hasItemMeta()) return;

        Boolean identified = item.getItemMeta().getPersistentDataContainer()
                .get(RPGKeys.Item.identified(), PersistentDataType.BOOLEAN);
        if (!Boolean.TRUE.equals(identified)) return;

        event.setCancelled(true);
        GemSocketService.Result result = GemSocketService.socket(item, gemId);
        switch (result) {
            case SUCCESS -> {
                ItemStack remaining = gem.clone();
                remaining.setAmount(gem.getAmount() - 1);
                event.getView().setCursor(remaining.getAmount() > 0 ? remaining : null);
                player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.4f);
                lang.send(player, "blacksmith.gem-socketed");
            }
            case NO_FREE_SLOTS -> lang.send(player, "blacksmith.gem-no-slots");
            case ALREADY_SOCKETED -> lang.send(player, "blacksmith.gem-already-socketed");
            case NOT_A_WEAPON -> lang.send(player, "blacksmith.gem-not-a-weapon");
        }
    }

    private void processIdentify(Player player, Inventory inventory) {
        ItemStack target = inventory.getItem(ITEM_SLOT);
        if (!isUnidentified(target)) {
            lang.send(player, "blacksmith.place-unidentified");
            return;
        }

        String rarityRaw = target.getItemMeta().getPersistentDataContainer()
                .get(RPGKeys.Item.rarity(), PersistentDataType.STRING);
        ItemRarity rarity;
        try {
            rarity = rarityRaw == null ? ItemRarity.COMMON : ItemRarity.valueOf(rarityRaw);
        } catch (IllegalArgumentException ignored) {
            rarity = ItemRarity.COMMON;
        }

        double cost = economyConfig.identificationCost(rarity);
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.removeMoney(cost)) {
            lang.send(player, "blacksmith.need-gold-identify", "cost", String.valueOf(cost));
            return;
        }

        inventory.setItem(ITEM_SLOT, RPGItemBuilder.identify(target));
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
        lang.send(player, "blacksmith.identified");
    }

    private void processSoulbound(Player player, Inventory inventory) {
        ItemStack target = inventory.getItem(ITEM_SLOT);
        if (target == null || target.getType() == Material.AIR || !target.hasItemMeta()) {
            lang.send(player, "blacksmith.place-identified");
            return;
        }

        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) return;

        int requiredLevel = economyConfig.getSoulboundMinLevel();
        if (profile.getLevel() < requiredLevel) {
            lang.send(player, "blacksmith.soulbound-requires-level", "level", String.valueOf(requiredLevel));
            return;
        }

        double cost = economyConfig.getSoulboundCost();
        if (!profile.removeMoney(cost)) {
            lang.send(player, "blacksmith.need-gold-soulbind", "cost", String.valueOf(cost));
            return;
        }

        switch (SoulboundService.apply(target)) {
            case SUCCESS -> {
                inventory.setItem(ITEM_SLOT, target);
                player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
                lang.send(player, "blacksmith.now-soulbound");
            }
            case ALREADY_SOULBOUND -> {
                profile.addMoney(cost);
                lang.send(player, "blacksmith.already-soulbound");
            }
            case NOT_IDENTIFIED -> {
                profile.addMoney(cost);
                lang.send(player, "blacksmith.only-identified-soulbind");
            }
        }
    }

    private boolean isUnidentified(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        Boolean identified = item.getItemMeta().getPersistentDataContainer()
                .get(RPGKeys.Item.identified(), PersistentDataType.BOOLEAN);
        return Boolean.FALSE.equals(identified);
    }

    // Gibt ein beim Schließen des Schmiedemenüs noch eingelegtes Item sicher an den Spieler zurück.
    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof BlacksmithHolder)) return;
        if (!(event.getPlayer() instanceof Player player)) return;

        ItemStack leftover = event.getInventory().getItem(ITEM_SLOT);
        if (leftover == null || leftover.getType() == Material.AIR) return;

        event.getInventory().setItem(ITEM_SLOT, null);
        player.getInventory().addItem(leftover).values()
                .forEach(remainder -> player.getWorld().dropItemNaturally(player.getLocation(), remainder));
    }
}
