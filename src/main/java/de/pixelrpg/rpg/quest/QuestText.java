package de.pixelrpg.rpg.quest;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.item.ItemDisplayNameResolver;
import de.pixelrpg.rpg.player.PlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;

/** Centralized player-facing text for every quest screen and notification. */
public final class QuestText {
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

    public static Component title(Quest quest) { return Component.text(titlePlain(quest)); }
    public static Component title(Player player, Quest quest) { return Component.text(titlePlain(quest)); }

    public static String titlePlain(Quest quest) {
        String title = quest.title();
        return title == null || title.isBlank() ? "Unbenannte Quest" : title;
    }

    public static String titlePlain(Player player, Quest quest) { return titlePlain(quest); }

    public static Component description(Quest quest) {
        String description = quest.description();
        return Component.text(description == null || description.isBlank() ? "Keine Beschreibung verfügbar." : description);
    }

    public static Component description(Player player, Quest quest) { return description(quest); }
    public static Component objective(Quest quest) { return objective(null, quest); }

    public static Component navigationTarget(Player player, Quest quest, PlayerProfile.NavigationTarget target) {
        if (target == null || target.worldId() == null) return Component.empty();
        String worldName = java.util.Optional.ofNullable(org.bukkit.Bukkit.getWorld(target.worldId()))
                .map(world -> world.getName())
                .orElse("unbekannte Welt");
        return Component.text("Quest-Ziel: ", NamedTextColor.AQUA)
                .append(Component.text("X " + formatCoordinate(target.x()) + "  Y " + formatCoordinate(target.y()) + "  Z " + formatCoordinate(target.z()), NamedTextColor.YELLOW))
                .append(Component.text(" • Welt: " + worldName, NamedTextColor.GRAY));
    }

    private static String formatCoordinate(double value) {
        return Long.toString(Math.round(value));
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

    public static Component objectiveWithProgress(Quest quest, QuestProgress progress) { return objectiveWithProgress(null, quest, progress); }

    public static Component objectiveWithProgress(Player player, Quest quest, QuestProgress progress) {
        int current = Math.min(Math.max(0, progress.getCurrentAmount()), quest.requiredAmount());
        NamedTextColor color = current >= quest.requiredAmount() ? NamedTextColor.GREEN : NamedTextColor.AQUA;
        return Component.text("Ziel: ", NamedTextColor.GRAY)
                .append(objective(player, quest).color(NamedTextColor.WHITE))
                .append(Component.text(" • Fortschritt: ", NamedTextColor.GRAY))
                .append(Component.text(current + "/" + quest.requiredAmount(), color));
    }

    public static Component requiredItem(Quest quest) { return requiredItem(null, quest); }

    public static Component requiredItem(Player player, Quest quest) {
        if (quest.type() != QuestType.COLLECT) return Component.empty();
        return Component.text("Benötigt: " + quest.requiredAmount() + "x ", NamedTextColor.AQUA).append(itemName(quest.targetKey()));
    }

    public static String requiredItemPlain(Quest quest) {
        if (quest.type() != QuestType.COLLECT) return "";
        return quest.requiredAmount() + "x " + itemNamePlain(quest.targetKey());
    }

    /** Resolves a quest item exclusively through the central item definition/recipe pipeline before vanilla fallback. */
    public static Component itemName(String key) {
        return Component.text(itemNamePlain(key));
    }

    public static String itemNamePlain(String key) {
        if (key == null || key.isBlank()) return "Unbekannt";

        ItemDisplayNameResolver resolver = PixelRPGPlugin.getInstance() == null
                ? null
                : PixelRPGPlugin.getInstance().getItemDisplayNameResolver();
        if (resolver != null) {
            var resolved = resolver.resolve(key);
            if (resolved.isPresent()) return resolved.get();
        }

        Material material = Material.matchMaterial(key.trim());
        if (material != null) return prettyKey(material.name());
        return prettyKey(key);
    }

    public static Component entityName(String key) { return Component.text(entityNamePlain(key)); }

    public static String entityNamePlain(String key) {
        if (key == null || key.isBlank()) return "Unbekannte Kreatur";
        String known = GERMAN_ENTITY_NAMES.get(key.trim().toUpperCase(Locale.ROOT));
        return known != null ? known : prettyKey(key);
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
