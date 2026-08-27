package de.pixelrpg.rpg.combat.loot;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.api.GuildAPI;
import de.pixelrpg.rpg.core.Level;
import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.economy.GuildCurrencyItemFactory;
import de.pixelrpg.rpg.item.ItemDefinition;
import de.pixelrpg.rpg.item.ItemEconomyConfig;
import de.pixelrpg.rpg.item.ItemRarity;
import de.pixelrpg.rpg.item.ItemService;
import de.pixelrpg.rpg.item.RPGItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public final class LootDropListener implements Listener {
    private static final List<Material> DROP_POOL = List.of(
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.GOLDEN_SWORD, Material.COPPER_AXE, Material.COPPER_SWORD,
            Material.IRON_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD, Material.MACE,
            Material.STONE_AXE, Material.IRON_AXE, Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE,
            Material.BOW, Material.CROSSBOW, Material.TRIDENT,
            Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS,
            Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS,
            Material.COPPER_HELMET, Material.COPPER_CHESTPLATE, Material.COPPER_LEGGINGS, Material.COPPER_BOOTS,
            Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS,
            Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE, Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS,
            Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS,
            Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS,
            Material.TURTLE_HELMET, Material.SHIELD, Material.IRON_PICKAXE, Material.DIAMOND_PICKAXE,
            Material.IRON_SHOVEL, Material.DIAMOND_SHOVEL, Material.IRON_HOE, Material.DIAMOND_HOE,
            Material.SHEARS, Material.FISHING_ROD
    );

    private final GuildAPI guildAPI;
    private final ItemEconomyConfig economyConfig;
    private final ItemService itemService;
    private final Map<Material, List<ItemDefinition>> definedItemsByMaterial;

    public LootDropListener(GuildAPI guildAPI, ItemEconomyConfig economyConfig) {
        this.guildAPI = guildAPI;
        this.economyConfig = economyConfig;
        this.itemService = PixelRPGPlugin.getInstance().getItemService();
        this.definedItemsByMaterial = itemService.definitions().stream()
                .filter(definition -> !definition.adminOnly())
                .filter(definition -> !definition.unique())
                .collect(Collectors.groupingBy(ItemDefinition::material, Collectors.toList()));
    }

    // Zuständig für PixelRPG-Loot und Gildengold; Vanilla-Spieler bleiben vollständig beim Vanilla-Loot.
    @EventHandler(priority = EventPriority.HIGH)
    public void onMonsterDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Monster)) return;
        Player killer = entity.getKiller();
        if (killer == null || !guildAPI.isRegistered(killer.getUniqueId())) return;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int playerLevel = Math.max(Level.MIN_LEVEL, Math.min(Level.MAX_NORMAL_LEVEL, guildAPI.getLevel(killer.getUniqueId())));
        applyArmorStatsToVanillaDrops(event.getDrops(), playerLevel, random);
        if (random.nextDouble() < economyConfig.getItemDropChance()) {
            int lootLevel = rollLootLevel(playerLevel, random);
            Material material = DROP_POOL.get(random.nextInt(DROP_POOL.size()));
            createLoot(material, lootLevel, playerLevel).ifPresent(event.getDrops()::add);
        }
        if (random.nextDouble() < economyConfig.getCurrencyDropChance()) {
            long min = economyConfig.getCurrencyDropMinAmount();
            long max = Math.max(min, economyConfig.getCurrencyDropMaxAmount());
            long amount = min == max ? min : random.nextLong(min, max + 1);
            event.getDrops().addAll(GuildCurrencyItemFactory.createStacks(amount));
        }
    }

    private void applyArmorStatsToVanillaDrops(List<ItemStack> drops, int playerLevel, ThreadLocalRandom random) {
        for (int index = 0; index < drops.size(); index++) {
            ItemStack vanilla = drops.get(index);
            if (vanilla == null || vanilla.isEmpty() || !isArmor(vanilla.getType()) || itemService.isRPGItem(vanilla)) continue;
            int itemLevel = rollLootLevel(playerLevel, random);
            ItemRarity rarity = ItemRarity.rollRandom();
            ItemStack rpgArmor = RPGItemBuilder.createItem(vanilla.getType(), rarity, itemLevel).orElse(null);
            if (rpgArmor == null) continue;
            applyRandomArmorAffixes(rpgArmor, itemLevel, rarity, random);
            rpgArmor.setAmount(vanilla.getAmount());
            drops.set(index, rpgArmor);
        }
    }

    private void applyRandomArmorAffixes(ItemStack item, int itemLevel, ItemRarity rarity, ThreadLocalRandom random) {
        List<String> candidates = new ArrayList<>(List.of("CRIT", "CRIT_DAMAGE", "LIFESTEAL", "DAMAGE", "REACH"));
        java.util.Collections.shuffle(candidates, new java.util.Random(random.nextLong()));
        int count = 1 + random.nextInt(3);
        ItemMeta meta = item.getItemMeta();
        var pdc = meta.getPersistentDataContainer();
        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        double rarityMultiplier = rarity.getStatMultiplier();
        double scale = 1.0D + Math.min(4.0D, Math.max(0, itemLevel - 1) * 0.04D);
        for (int i = 0; i < count; i++) {
            switch (candidates.get(i)) {
                case "CRIT" -> {
                    double value = round((0.6D + random.nextDouble() * 1.8D) * rarityMultiplier * scale / 10.0D);
                    pdc.set(RPGKeys.Item.critChance(), PersistentDataType.DOUBLE, value);
                    lore.add(Component.text("+" + format(value) + "% Crit", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
                }
                case "CRIT_DAMAGE" -> {
                    double value = round((0.05D + random.nextDouble() * 0.15D) * rarityMultiplier * scale);
                    pdc.set(RPGKeys.Item.critDamage(), PersistentDataType.DOUBLE, value);
                    lore.add(Component.text("+" + format(value * 100.0D) + "% Crit-Schaden", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
                }
                case "LIFESTEAL" -> {
                    double value = round((0.25D + random.nextDouble() * 1.25D) * rarityMultiplier * scale / 10.0D);
                    pdc.set(RPGKeys.Item.lifestealPercent(), PersistentDataType.DOUBLE, value);
                    lore.add(Component.text("+" + format(value) + "% Lifesteal", NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false));
                }
                case "DAMAGE" -> {
                    double value = round((0.5D + random.nextDouble() * 1.5D) * rarityMultiplier * scale / 4.0D);
                    pdc.set(RPGKeys.Item.bonusDamage(), PersistentDataType.DOUBLE, value);
                    lore.add(Component.text("+" + format(value) + " Damage", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
                }
                case "REACH" -> {
                    double value = round((0.05D + random.nextDouble() * 0.15D) * rarityMultiplier);
                    pdc.set(RPGKeys.Item.reachBonus(), PersistentDataType.DOUBLE, value);
                    lore.add(Component.text("+" + format(value) + " Reach", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
                }
                default -> { }
            }
        }
        meta.lore(lore);
        item.setItemMeta(meta);
    }

    private java.util.Optional<ItemStack> createLoot(Material material, int lootLevel, int playerLevel) {
        ItemDefinition definition = definedItemsByMaterial.getOrDefault(material, List.of()).stream()
                .filter(candidate -> candidate.itemLevel() <= lootLevel)
                .filter(candidate -> candidate.requiredLevel() <= playerLevel)
                .max(java.util.Comparator.comparingInt(ItemDefinition::itemLevel))
                .orElse(null);
        if (definition != null) return itemService.createItem(definition.id());
        int itemLevel = Math.max(Level.MIN_LEVEL, Math.min(lootLevel, playerLevel));
        return RPGItemBuilder.createItem(material, ItemRarity.rollRandom(), itemLevel);
    }

    private int rollLootLevel(int playerLevel, ThreadLocalRandom random) {
        long totalWeight = 0L;
        for (int level = Level.MIN_LEVEL; level <= playerLevel; level++) totalWeight += levelWeight(level);
        long roll = random.nextLong(totalWeight);
        for (int level = Level.MIN_LEVEL; level <= playerLevel; level++) {
            roll -= levelWeight(level);
            if (roll < 0L) return level;
        }
        return playerLevel;
    }

    private long levelWeight(int itemLevel) { long level = Math.max(1L, itemLevel); return level * level; }
    private static boolean isArmor(Material material) { return material.name().endsWith("_HELMET") || material.name().endsWith("_CHESTPLATE") || material.name().endsWith("_LEGGINGS") || material.name().endsWith("_BOOTS"); }
    private static double round(double value) { return Math.round(value * 100.0D) / 100.0D; }
    private static String format(double value) { return Math.abs(value - Math.rint(value)) < 0.0001D ? Long.toString(Math.round(value)) : String.format(java.util.Locale.ROOT, "%.2f", value); }
}
