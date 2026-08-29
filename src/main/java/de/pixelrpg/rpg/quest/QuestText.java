package de.pixelrpg.rpg.quest;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.item.ItemDefinition;
import de.pixelrpg.rpg.item.ItemDefinitionRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;

/** Centralized player-facing text for every quest screen and notification. */
public final class QuestText {
    private static final Map<String, String> GERMAN_ITEM_NAMES = Map.ofEntries(
            Map.entry("OAK_LOG", "Eichenstamm"), Map.entry("HONEYCOMB", "Honigwabe"),
            Map.entry("COPPER_ORE", "Kupfererz"), Map.entry("IRON_INGOT", "Eisenbarren"),
            Map.entry("GOLD_INGOT", "Goldbarren"), Map.entry("BLAZE_ROD", "Lohenrute"),
            Map.entry("ENDER_PEARL", "Enderperle"), Map.entry("CHORUS_FRUIT", "Chorusfrucht"),
            Map.entry("NETHERITE_SCRAP", "Netherit-Schrott"), Map.entry("DIAMOND", "Diamant"),
            Map.entry("COAL", "Kohle"), Map.entry("WHEAT", "Weizen"),
            Map.entry("BEEF", "Rindfleisch"), Map.entry("COD", "Kabeljau"),
            Map.entry("SUGAR", "Zucker"), Map.entry("APPLE", "Apfel"),
            Map.entry("DANDELION", "Löwenzahn"), Map.entry("RED_DYE", "Roter Farbstoff"),
            Map.entry("GLOW_BERRIES", "Leuchtbeeren"), Map.entry("SPIDER_EYE", "Spinnenauge"),
            Map.entry("GHAST_TEAR", "Ghastträne"), Map.entry("PAPER", "Papier"),
            Map.entry("INK_SAC", "Tintenbeutel"), Map.entry("BOOK", "Buch"),
            Map.entry("OBSIDIAN", "Obsidian"), Map.entry("SLIME_BALL", "Schleimball"),
            Map.entry("REDSTONE", "Redstone"), Map.entry("LAPIS_LAZULI", "Lapislazuli"),
            Map.entry("QUARTZ", "Netherquarz"), Map.entry("ANCIENT_DEBRIS", "Antiker Schrott"),
            Map.entry("GLOWSTONE_DUST", "Leuchtsteinstaub"), Map.entry("PRISMARINE_SHARD", "Prismarinscherbe"),
            Map.entry("PRISMARINE_CRYSTALS", "Prismarinkristall"), Map.entry("SOUL_SAND", "Seelensand"),
            Map.entry("EMERALD", "Smaragd"), Map.entry("ENDER_EYE", "Enderauge"),
            Map.entry("NETHERITE_INGOT", "Netheritbarren"), Map.entry("DRAGON_BREATH", "Drachenatem"),
            Map.entry("AMETHYST_SHARD", "Amethystscherbe"), Map.entry("NETHER_STAR", "Netherstern"),
            Map.entry("MAGMA_CREAM", "Magmacreme"), Map.entry("FIRE_CHARGE", "Feuerkugel"),
            Map.entry("GUNPOWDER", "Schießpulver"), Map.entry("TORCH", "Fackel"),
            Map.entry("ARROW", "Pfeil"), Map.entry("STRING", "Faden"), Map.entry("SLIME", "Schleim"),
            Map.entry("QUARTZ", "Netherquarz"), Map.entry("PHANTOM_MEMBRANE", "Phantomhaut"),
            Map.entry("SHULKER_SHELL", "Shulkerschale"), Map.entry("COOKED_BEEF", "Gebratenes Rindfleisch"),
            Map.entry("COOKED_PORKCHOP", "Gebratenes Schweinefleisch"), Map.entry("EYE_OF_ENDER", "Enderauge"),
            Map.entry("NETHERITE_SCRAP", "Netherit-Schrott"), Map.entry("NETHERITE_INGOT", "Netheritbarren"),
            Map.entry("BAMBOO", "Bambus")
    );

    private static final Map<String, String> GERMAN_ENTITY_NAMES = Map.ofEntries(
            Map.entry("ZOMBIE", "Zombie"), Map.entry("SKELETON", "Skelett"), Map.entry("SPIDER", "Spinne"),
            Map.entry("WITCH", "Hexe"), Map.entry("CHICKEN", "Huhn"), Map.entry("COW", "Kuh"),
            Map.entry("SHEEP", "Schaf"), Map.entry("RABBIT", "Kaninchen"), Map.entry("BAT", "Fledermaus"),
            Map.entry("FOX", "Fuchs"), Map.entry("GOAT", "Ziege"), Map.entry("PARROT", "Papagei"),
            Map.entry("ARMADILLO", "Gürteltier"), Map.entry("PANDA", "Panda"), Map.entry("BOGGED", "Sumpfskelett"),
            Map.entry("CREEPER", "Creeper"), Map.entry("HUSK", "Wüstenzombie"), Map.entry("DROWNED", "Ertrunkener"),
            Map.entry("SLIME", "Schleim"), Map.entry("ENDERMAN", "Enderman"), Map.entry("BLAZE", "Lohe"),
            Map.entry("GHAST", "Ghast"), Map.entry("MAGMA_CUBE", "Magmawürfel"), Map.entry("PIGLIN", "Piglin"),
            Map.entry("PIGLIN_BRUTE", "Piglin-Barbar"), Map.entry("HOGLIN", "Hoglin"), Map.entry("WITHER_SKELETON", "Witherskelett"),
            Map.entry("GUARDIAN", "Wächter"), Map.entry("ELDER_GUARDIAN", "Ältester Wächter"), Map.entry("SHULKER", "Shulker"),
            Map.entry("EVOKER", "Magier"), Map.entry("VINDICATOR", "Vindicator"), Map.entry("RAVAGER", "Verwüster"),
            Map.entry("WARDEN", "Wärter"), Map.entry("PHANTOM", "Phantom"), Map.entry("WITHER", "Wither"),
            Map.entry("CREAKING", "Knarzer")
    );

    private QuestText() {
    }

    public static Component title(Quest quest) {
        return Component.text(titlePlain(quest));
    }

    public static Component title(Player player, Quest quest) {
        return Component.text(titlePlain(quest));
    }

    public static String titlePlain(Quest quest) {
        String title = quest.title();
        return title == null || title.isBlank() ? "Unbenannte Quest" : title;
    }

    public static String titlePlain(Player player, Quest quest) {
        return titlePlain(quest);
    }

    public static Component description(Quest quest) {
        String description = quest.description();
        return Component.text(description == null || description.isBlank() ? "Keine Beschreibung verfügbar." : description);
    }

    public static Component description(Player player, Quest quest) {
        return description(quest);
    }

    public static Component objective(Quest quest) {
        return objective(null, quest);
    }

    public static Component objective(Player player, Quest quest) {
        String amount = String.valueOf(quest.requiredAmount());
        return switch (quest.type()) {
            case HUNT -> Component.text("Töte " + amount + "x ", NamedTextColor.WHITE).append(entityName(quest.targetKey()));
            case COLLECT -> Component.text("Sammle " + amount + "x ", NamedTextColor.WHITE).append(itemName(quest.targetKey()));
            case TALK_TO_NPC -> Component.text("Sprich mit " + prettyKey(quest.targetKey()), NamedTextColor.WHITE);
            case REACH_LOCATION -> Component.text("Erreiche den angegebenen Zielort.", NamedTextColor.WHITE);
            case GLOBAL_EVENT -> Component.text("Beteilige dich am serverweiten Ziel: " + prettyKey(quest.targetKey()), NamedTextColor.WHITE);
        };
    }

    public static Component objectiveWithProgress(Quest quest, QuestProgress progress) {
        return objectiveWithProgress(null, quest, progress);
    }

    public static Component objectiveWithProgress(Player player, Quest quest, QuestProgress progress) {
        int current = Math.min(Math.max(0, progress.getCurrentAmount()), quest.requiredAmount());
        NamedTextColor color = current >= quest.requiredAmount() ? NamedTextColor.GREEN : NamedTextColor.AQUA;
        return Component.text("Ziel: ", NamedTextColor.GRAY)
                .append(objective(player, quest).color(NamedTextColor.WHITE))
                .append(Component.text(" • Fortschritt: ", NamedTextColor.GRAY))
                .append(Component.text(current + "/" + quest.requiredAmount(), color));
    }

    public static Component requiredItem(Quest quest) {
        return requiredItem(null, quest);
    }

    public static Component requiredItem(Player player, Quest quest) {
        if (quest.type() != QuestType.COLLECT) return Component.empty();
        return Component.text("Benötigt: " + quest.requiredAmount() + "x ", NamedTextColor.AQUA)
                .append(itemName(quest.targetKey()));
    }

    public static String requiredItemPlain(Quest quest) {
        if (quest.type() != QuestType.COLLECT) return "";
        return quest.requiredAmount() + "x " + itemNamePlain(quest.targetKey());
    }

    /** Resolves a quest item to the actual PixelRPG display name or a fixed German vanilla name. */
    public static Component itemName(String key) {
        ItemDefinition definition = findDefinition(key);
        if (definition != null) return Component.text(definition.name());
        return Component.text(itemNamePlain(key));
    }

    /** Resolves a quest item to a plain display name for string-only contexts. */
    public static String itemNamePlain(String key) {
        if (key == null || key.isBlank()) return "Unbekannt";
        String normalized = key.trim().toUpperCase(Locale.ROOT);
        String known = GERMAN_ITEM_NAMES.get(normalized);
        if (known != null) return known;

        ItemDefinition definition = findDefinition(key);
        if (definition != null) return definition.name();

        Material material = Material.matchMaterial(key.trim());
        if (material != null) return prettyKey(material.name());
        return prettyKey(key);
    }

    public static Component entityName(String key) {
        return Component.text(entityNamePlain(key));
    }

    public static String entityNamePlain(String key) {
        if (key == null || key.isBlank()) return "Unbekannte Kreatur";
        String known = GERMAN_ENTITY_NAMES.get(key.trim().toUpperCase(Locale.ROOT));
        return known != null ? known : prettyKey(key);
    }

    private static ItemDefinition findDefinition(String key) {
        if (key == null || key.isBlank()) return null;
        try {
            return new ItemDefinitionRegistry(PixelRPGPlugin.getInstance()).find(key).orElse(null);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String prettyKey(String key) {
        if (key == null || key.isBlank()) return "Unbekannt";
        String value = key.trim();
        int separator = value.indexOf(':');
        if (separator >= 0 && separator + 1 < value.length()) value = value.substring(separator + 1);
        value = value.toLowerCase(Locale.ROOT).replace('_', ' ').replace('-', ' ');
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
