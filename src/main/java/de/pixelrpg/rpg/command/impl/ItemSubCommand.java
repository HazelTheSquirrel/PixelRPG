package de.pixelrpg.rpg.command.impl;

import de.pixelrpg.rpg.command.SubCommand;
import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.item.ItemDefinition;
import de.pixelrpg.rpg.item.ItemRarity;
import de.pixelrpg.rpg.item.ItemService;
import de.pixelrpg.rpg.item.RPGItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Admin-only command for inspecting, generating and tuning concrete PixelRPG items. */
public final class ItemSubCommand implements SubCommand {
    private static final String FIREBALL_ADMIN_ID = "pixelrpg:weapons/feuerball/common_1";
    private static final String FIREBALL_NAME = "Feuerball";
    private static final long FIREBALL_COOLDOWN_MILLIS = 3_000L;

    private final ItemService itemService;

    public ItemSubCommand(ItemService itemService) { this.itemService = itemService; }
    @Override public String name() { return "item"; }
    @Override public String permission() { return "rpg.admin"; }
    @Override public String description() { return "PixelRPG-Items verwalten"; }
    @Override public String usage() { return "/pixelrpg item <list|give|create|inspect|set> ..."; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) return false;
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list" -> list(sender);
            case "give" -> give(sender, args);
            case "create" -> create(sender, args);
            case "inspect" -> inspect(sender, args);
            case "set" -> set(sender, args);
            default -> false;
        };
    }

    private boolean list(CommandSender sender) {
        itemService.definitions().stream().map(ItemDefinition::id).sorted().forEach(id -> sender.sendMessage(Component.text(id, NamedTextColor.YELLOW)));
        sender.sendMessage(Component.text(FIREBALL_ADMIN_ID, NamedTextColor.YELLOW));
        return true;
    }

    private boolean give(CommandSender sender, String[] args) {
        if (args.length < 3) return false;
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { sender.sendMessage(Component.text("Spieler nicht online.", NamedTextColor.RED)); return true; }
        int amount = 1;
        if (args.length >= 4) {
            try { amount = Integer.parseInt(args[3]); }
            catch (NumberFormatException exception) { sender.sendMessage(Component.text("Ungültige Anzahl.", NamedTextColor.RED)); return true; }
        }
        if (amount < 1 || amount > 64) { sender.sendMessage(Component.text("Anzahl muss zwischen 1 und 64 liegen.", NamedTextColor.RED)); return true; }

        if (isFireballId(args[2])) {
            if (amount != 1) {
                sender.sendMessage(Component.text("Der Feuerball wird als einzelne Waffe vergeben.", NamedTextColor.RED));
                return true;
            }
            ItemStack fireball = createAdminFireball();
            if (fireball == null) {
                sender.sendMessage(Component.text("Feuerball konnte nicht erstellt werden.", NamedTextColor.RED));
                return true;
            }
            target.getInventory().addItem(fireball).values().forEach(stack -> target.getWorld().dropItemNaturally(target.getLocation(), stack));
            sender.sendMessage(Component.text("Item vergeben: Feuerball", NamedTextColor.GREEN));
            return true;
        }

        ItemDefinition definition = itemService.definitions().stream().filter(value -> value.id().equalsIgnoreCase(args[2]) || value.id().equalsIgnoreCase("pixelrpg:" + args[2])).findFirst().orElse(null);
        if (definition == null) { sender.sendMessage(Component.text("Unbekannte Item-ID.", NamedTextColor.RED)); return true; }
        if (definition.unique() && amount != 1) { sender.sendMessage(Component.text("UNIQUE-Items können nur einzeln vergeben werden.", NamedTextColor.RED)); return true; }
        ItemStack item = itemService.createAdminItem(definition.id()).orElse(null);
        if (item == null) { sender.sendMessage(Component.text("Item konnte nicht erstellt oder UNIQUE bereits vergeben werden.", NamedTextColor.RED)); return true; }
        item.setAmount(amount);
        target.getInventory().addItem(item).values().forEach(stack -> target.getWorld().dropItemNaturally(target.getLocation(), stack));
        sender.sendMessage(Component.text("Item vergeben: " + definition.id(), NamedTextColor.GREEN));
        return true;
    }

    private ItemStack createAdminFireball() {
        ItemStack item = RPGItemBuilder.createItem(FIREBALL_ADMIN_ID, FIREBALL_NAME, Material.FIRE_CHARGE, ItemRarity.COMMON, 1).orElse(null);
        if (item == null) return null;
        item = RPGItemBuilder.withWeaponAbility(item, FIREBALL_NAME, FIREBALL_COOLDOWN_MILLIS);
        ItemMeta meta = item.getItemMeta();
        var pdc = meta.getPersistentDataContainer();
        pdc.set(RPGKeys.Item.itemId(), PersistentDataType.STRING, FIREBALL_ADMIN_ID);
        pdc.set(RPGKeys.Item.identified(), PersistentDataType.BOOLEAN, true);
        meta.displayName(Component.text(FIREBALL_NAME, NamedTextColor.WHITE));
        item.setItemMeta(meta);
        return item;
    }

    private static boolean isFireballId(String value) {
        return value.equalsIgnoreCase("feuerball")
                || value.equalsIgnoreCase("fireball")
                || value.equalsIgnoreCase(FIREBALL_ADMIN_ID);
    }

    private boolean create(CommandSender sender, String[] args) {
        if (args.length < 5) return false;
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { sender.sendMessage(Component.text("Spieler nicht online.", NamedTextColor.RED)); return true; }
        Material material = Material.matchMaterial(args[2]);
        ItemRarity rarity;
        int level;
        try { rarity = ItemRarity.valueOf(args[3].toUpperCase(Locale.ROOT)); level = Integer.parseInt(args[4]); }
        catch (IllegalArgumentException exception) { sender.sendMessage(Component.text("Ungültiges Material, Rarity oder Level.", NamedTextColor.RED)); return true; }
        ItemStack item = RPGItemBuilder.createItem(material, rarity, level).orElse(null);
        if (item == null) { sender.sendMessage(Component.text("Aus diesem Material kann kein PixelRPG-Item erzeugt werden.", NamedTextColor.RED)); return true; }
        target.getInventory().addItem(item).values().forEach(stack -> target.getWorld().dropItemNaturally(target.getLocation(), stack));
        sender.sendMessage(Component.text("Generiertes Item erstellt und vergeben.", NamedTextColor.GREEN));
        return true;
    }

    private boolean inspect(CommandSender sender, String[] args) {
        if (args.length < 2) return false;
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { sender.sendMessage(Component.text("Spieler nicht online.", NamedTextColor.RED)); return true; }
        ItemStack item = target.getInventory().getItemInMainHand();
        if (item.isEmpty()) { sender.sendMessage(Component.text("Spieler hält kein Item.", NamedTextColor.RED)); return true; }
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        sender.sendMessage(Component.text("ITEM " + item.getType() + " x" + item.getAmount(), NamedTextColor.GOLD));
        sender.sendMessage(Component.text("id=" + itemService.getItemId(item).orElse("-") + " rarity=" + itemService.getRarity(item).map(Enum::name).orElse("-") + " category=" + itemService.getCategory(item).map(Enum::name).orElse("-"), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("level=" + value(pdc, RPGKeys.Item.itemLevel(), PersistentDataType.INTEGER) + " required=" + itemService.getRequiredLevel(item).orElse(-1) + " gearscore=" + itemService.getGearscore(item).orElse(-1.0D), NamedTextColor.GRAY));
        return true;
    }

    private boolean set(CommandSender sender, String[] args) {
        if (args.length < 4) return false;
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { sender.sendMessage(Component.text("Spieler nicht online.", NamedTextColor.RED)); return true; }
        ItemStack item = target.getInventory().getItemInMainHand();
        if (item.isEmpty()) { sender.sendMessage(Component.text("Spieler hält kein Item.", NamedTextColor.RED)); return true; }
        double amount;
        try { amount = Double.parseDouble(args[3]); }
        catch (NumberFormatException exception) { sender.sendMessage(Component.text("Ungültiger Wert.", NamedTextColor.RED)); return true; }
        if (!Double.isFinite(amount) || amount < 0.0D) { sender.sendMessage(Component.text("Wert muss >= 0 sein.", NamedTextColor.RED)); return true; }
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String key = args[2].toLowerCase(Locale.ROOT);
        switch (key) {
            case "level" -> pdc.set(RPGKeys.Item.itemLevel(), PersistentDataType.INTEGER, (int) amount);
            case "requiredlevel" -> pdc.set(RPGKeys.Item.requiredLevel(), PersistentDataType.INTEGER, (int) amount);
            case "attackpower" -> pdc.set(RPGKeys.Item.attackPower(), PersistentDataType.DOUBLE, amount);
            case "crit" -> pdc.set(RPGKeys.Item.critChance(), PersistentDataType.DOUBLE, amount);
            case "critdamage" -> pdc.set(RPGKeys.Item.critDamage(), PersistentDataType.DOUBLE, amount);
            case "reach" -> pdc.set(RPGKeys.Item.reachBonus(), PersistentDataType.DOUBLE, amount);
            case "lifesteal" -> pdc.set(RPGKeys.Item.lifestealPercent(), PersistentDataType.DOUBLE, amount);
            case "armor" -> pdc.set(RPGKeys.Item.armorValue(), PersistentDataType.DOUBLE, amount);
            case "health" -> pdc.set(RPGKeys.Item.healthBonus(), PersistentDataType.DOUBLE, amount);
            case "movement" -> pdc.set(RPGKeys.Item.movementSpeed(), PersistentDataType.DOUBLE, amount);
            case "gearscore" -> pdc.set(RPGKeys.Item.gearscore(), PersistentDataType.DOUBLE, amount);
            default -> { sender.sendMessage(Component.text("Unbekannter Item-Stat.", NamedTextColor.RED)); return true; }
        }
        item.setItemMeta(meta);
        target.getInventory().setItemInMainHand(item);
        sender.sendMessage(Component.text("Item-Stat " + key + " auf " + amount + " gesetzt.", NamedTextColor.GREEN));
        return true;
    }

    private <P, C> C value(PersistentDataContainer pdc, org.bukkit.NamespacedKey key, PersistentDataType<P, C> type) {
        return pdc.get(key, type);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) return List.of("list", "give", "create", "inspect", "set");
        if (args.length == 2) return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return java.util.stream.Stream.concat(itemService.definitions().stream().map(ItemDefinition::id), java.util.stream.Stream.of("feuerball")).toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("create")) return Arrays.stream(Material.values()).filter(Material::isItem).map(Enum::name).toList();
        if (args.length == 4 && args[0].equalsIgnoreCase("create")) return Arrays.stream(ItemRarity.values()).map(Enum::name).toList();
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) return List.of("level", "requiredLevel", "attackPower", "crit", "critDamage", "reach", "lifesteal", "armor", "health", "movement", "gearscore");
        return List.of();
    }
}
