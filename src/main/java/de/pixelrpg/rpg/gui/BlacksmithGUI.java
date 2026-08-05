package de.pixelrpg.rpg.gui;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.item.ItemEconomyConfig;
import de.pixelrpg.rpg.item.ItemRarity;
import de.pixelrpg.rpg.item.RPGItemBuilder;
import de.pixelrpg.rpg.item.RuneType;
import de.pixelrpg.rpg.item.SocketService;
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

        Inventory inv = Bukkit.createInventory(
                holder,
                27,
                lang.get("blacksmith.gui-title").color(NamedTextColor.DARK_GRAY)
        );

        holder.inventory = inv;

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);

        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.text(" "));
        filler.setItemMeta(fillerMeta);

        for (int i = 0; i < 27; i++) {
            if (i != ITEM_SLOT && i != IDENTIFY_BUTTON && i != SOULBOUND_BUTTON) {
                inv.setItem(i, filler);
            }
        }

        inv.setItem(
                IDENTIFY_BUTTON,
                buildButton(
                        Material.ANVIL,
                        "blacksmith.identify-button",
                        NamedTextColor.YELLOW,
                        "blacksmith.identify-desc"
                )
        );

        inv.setItem(
                SOULBOUND_BUTTON,
                buildButton(
                        Material.SOUL_SAND,
                        "blacksmith.soulbind-button",
                        NamedTextColor.LIGHT_PURPLE,
                        "blacksmith.soulbind-desc"
                )
        );

        player.openInventory(inv);
    }


    private ItemStack buildButton(Material material, String nameKey,
                                  NamedTextColor color,
                                  String descriptionKey) {

        ItemStack item = new ItemStack(material);

        ItemMeta meta = item.getItemMeta();

        meta.displayName(
                lang.get(nameKey).color(color)
                        .decoration(TextDecoration.ITALIC, false)
        );

        meta.lore(
                List.of(
                        lang.get(descriptionKey).color(NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false)
                )
        );

        item.setItemMeta(meta);

        return item;
    }


    @EventHandler
    public void onDrag(InventoryDragEvent event) {

        if (!(event.getInventory().getHolder() instanceof BlacksmithHolder)) {
            return;
        }

        for (int slot : event.getRawSlots()) {

            if (slot < event.getView().getTopInventory().getSize()
                    && slot != ITEM_SLOT) {

                event.setCancelled(true);
                return;
            }
        }
    }


    @EventHandler
    public void onClick(InventoryClickEvent event) {

        if (!(event.getInventory().getHolder() instanceof BlacksmithHolder holder)) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }


        Inventory topInventory = holder.getInventory();

        boolean clickedTop = event.getClickedInventory() == topInventory;


        if (event.getClick().isShiftClick()) {

            event.setCancelled(true);
            return;
        }


        if (clickedTop) {

            int slot = event.getSlot();


            if (slot == IDENTIFY_BUTTON) {

                event.setCancelled(true);
                processIdentify(player, topInventory);
                return;
            }


            if (slot == SOULBOUND_BUTTON) {

                event.setCancelled(true);
                processSoulbound(player, topInventory);
                return;
            }


            if (slot != ITEM_SLOT) {

                event.setCancelled(true);
                return;
            }


            ItemStack cursor = event.getCursor();
            ItemStack current = event.getCurrentItem();


            if (cursor != null
                    && cursor.getType() != Material.AIR
                    && current != null
                    && current.getType() != Material.AIR
                    && current.hasItemMeta()) {


                String runeTypeRaw =
                        cursor.hasItemMeta()
                                ? cursor.getItemMeta()
                                .getPersistentDataContainer()
                                .get(
                                        RPGKeys.Item.runeType(),
                                        PersistentDataType.STRING
                                )
                                : null;


                boolean isGearItem =
                        current.getItemMeta()
                                .getPersistentDataContainer()
                                .has(
                                        RPGKeys.Item.maxSockets(),
                                        PersistentDataType.INTEGER
                                );


                if (runeTypeRaw != null && isGearItem) {

                    event.setCancelled(true);

                    applySocket(
                            event,
                            player,
                            cursor,
                            current
                    );
                }


                // ============================
                // GEM SOCKETING
                // ============================

                if (event.getAction()
                        == org.bukkit.event.inventory.InventoryAction.SWAP_WITH_CURSOR) {

                    handlePotentialGemSocket(
                            event,
                            player,
                            cursor,
                            current
                    );
                }
            }
        }
    }

    private void handlePotentialGemSocket(org.bukkit.event.inventory.InventoryClickEvent event, Player player,
                                           org.bukkit.inventory.ItemStack cursor, org.bukkit.inventory.ItemStack clicked) {
        String gemId = de.pixelrpg.rpg.combat.gem.GemItemFactory.readGemId(cursor);
        if (gemId == null || clicked == null || !clicked.hasItemMeta()) {
            return;
        }

        Boolean identified = clicked.getItemMeta().getPersistentDataContainer()
                .get(RPGKeys.Item.identified(), org.bukkit.persistence.PersistentDataType.BOOLEAN);
        if (!Boolean.TRUE.equals(identified)) {
            return;
        }

        event.setCancelled(true);
        de.pixelrpg.rpg.combat.gem.GemSocketService.Result result =
                de.pixelrpg.rpg.combat.gem.GemSocketService.socket(clicked, gemId);

        switch (result) {
            case SUCCESS -> {
                org.bukkit.inventory.ItemStack newCursor = cursor.clone();
                newCursor.setAmount(cursor.getAmount() - 1);
                event.getView().setCursor(newCursor.getAmount() > 0 ? newCursor : null);
                player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.4f);
                lang.send(player, "blacksmith.gem-socketed");
            }
            case NO_FREE_SLOTS -> lang.send(player, "blacksmith.gem-no-slots");
            case ALREADY_SOCKETED -> lang.send(player, "blacksmith.gem-already-socketed");
            case NOT_A_WEAPON -> lang.send(player, "blacksmith.gem-not-a-weapon");
        }
    }

    private void applySocket(InventoryClickEvent event,
                             Player player,
                             ItemStack cursor,
                             ItemStack clicked) {


        var cursorPdc =
                cursor.getItemMeta()
                        .getPersistentDataContainer();


        String runeTypeRaw =
                cursorPdc.get(
                        RPGKeys.Item.runeType(),
                        PersistentDataType.STRING
                );


        Double runeValue =
                cursorPdc.get(
                        RPGKeys.Item.runeValue(),
                        PersistentDataType.DOUBLE
                );


        if (runeTypeRaw == null || runeValue == null) {
            return;
        }


        RuneType runeType =
                RuneType.valueOf(runeTypeRaw);


        SocketService.Result result =
                SocketService.applyRune(
                        clicked,
                        runeType,
                        runeValue
                );


        switch (result) {

            case SUCCESS -> {

                ItemStack newCursor =
                        cursor.clone();


                newCursor.setAmount(
                        cursor.getAmount() - 1
                );


                event.getView()
                        .setCursor(
                                newCursor.getAmount() > 0
                                        ? newCursor
                                        : null
                        );


                player.playSound(
                        player.getLocation(),
                        Sound.BLOCK_ENCHANTMENT_TABLE_USE,
                        1.0f,
                        1.2f
                );


                lang.send(player, "blacksmith.rune-socketed");
            }


            case NO_FREE_SLOTS -> lang.send(player, "blacksmith.rune-no-slots");


            case INCOMPATIBLE_RUNE -> lang.send(player, "blacksmith.rune-incompatible");


            case NOT_IDENTIFIED -> lang.send(player, "blacksmith.rune-not-identified");
        }
    }



    private void processIdentify(Player player,
                                 Inventory topInventory) {


        ItemStack target =
                topInventory.getItem(ITEM_SLOT);


        if (!isUnidentified(target)) {

            lang.send(player, "blacksmith.place-unidentified");

            return;
        }


        String rarityRaw =
                target.getItemMeta()
                        .getPersistentDataContainer()
                        .get(
                                RPGKeys.Item.rarity(),
                                PersistentDataType.STRING
                        );


        ItemRarity rarity =
                rarityRaw != null
                        ? ItemRarity.valueOf(rarityRaw)
                        : ItemRarity.COMMON;


        double cost =
                economyConfig.identificationCost(rarity);


        PlayerProfile profile =
                profileManager
                        .getProfile(player.getUniqueId())
                        .orElse(null);


        if (profile == null || !profile.removeMoney(cost)) {

            lang.send(player, "blacksmith.need-gold-identify", "cost", String.valueOf(cost));

            return;
        }


        ItemStack identified =
                RPGItemBuilder.identify(target);


        topInventory.setItem(
                ITEM_SLOT,
                identified
        );


        player.playSound(
                player.getLocation(),
                Sound.BLOCK_ANVIL_USE,
                1.0f,
                1.0f
        );


        lang.send(player, "blacksmith.identified");
    }



    private void processSoulbound(Player player,
                                  Inventory topInventory) {

        ItemStack target =
                topInventory.getItem(ITEM_SLOT);


        if (target == null
                || target.getType() == Material.AIR
                || !target.hasItemMeta()) {


            lang.send(player, "blacksmith.place-identified");
            return;
        }


        PlayerProfile profile =
                profileManager
                        .getProfile(player.getUniqueId())
                        .orElse(null);


        if (profile == null) {
            return;
        }

        if (!profile.getRank()
                .isAtLeast(economyConfig.getSoulboundMinRank())) {

            lang.send(player, "blacksmith.soulbound-requires-rank",
                    "rank", economyConfig.getSoulboundMinRank().name());
            return;
        }

        double cost =
                economyConfig.getSoulboundCost();


        if (!profile.removeMoney(cost)) {
            lang.send(player, "blacksmith.need-gold-soulbind", "cost", String.valueOf(cost));
            return;
        }

        SoulboundService.Result result =
                SoulboundService.apply(target);

        switch (result) {

            case SUCCESS -> {

                topInventory.setItem(
                        ITEM_SLOT,
                        target
                );

                player.playSound(
                        player.getLocation(),
                        Sound.PARTICLE_SOUL_ESCAPE,
                        1.0f,
                        1.0f
                );

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

        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        Boolean identified =
                item.getItemMeta()
                        .getPersistentDataContainer()
                        .get(RPGKeys.Item.identified(),
                             PersistentDataType.BOOLEAN);
        return Boolean.FALSE.equals(identified);
    }
}