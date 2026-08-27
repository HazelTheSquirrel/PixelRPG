package de.pixelrpg.rpg.quest;

import de.pixelrpg.rpg.item.ItemDefinition;
import de.pixelrpg.rpg.item.ItemDefinitionRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.Locale;

/** Centralized player-facing text for every quest screen and notification. */
public final class QuestText {
    private QuestText() {
    }

    public static Component title(Quest quest) {
        return Component.text(titlePlain(quest));
    }

    public static String titlePlain(Quest quest) {
        String title = quest.title();
        return title == null || title.isBlank() ? "Unbenannte Quest" : title;
    }

    public static Component description(Quest quest) {
        String description = quest.description();
        return Component.text(description == null || description.isBlank() ? "Keine Beschreibung vorhanden." : description);
    }

    public static Component objective(Quest quest) {
        return switch (quest.type()) {
            case HUNT -> Component.text("Töte ")
                    .append(Component.text(quest.requiredAmount()))
                    .append(Component.text("x "))
                    .append(entityName(quest.targetKey()));
            case COLLECT -> Component.text("Sammle ")
                    .append(Component.text(quest.requiredAmount()))
                    .append(Component.text("x "))
                    .append(itemName(quest.targetKey()));
            case TALK_TO_NPC -> Component.text("Sprich mit ").append(Component.text(prettyKey(quest.targetKey())));
            case REACH_LOCATION -> Component.text("Erreiche den angegebenen Zielort.");
            case GLOBAL_EVENT -> Component.text("Beteilige dich am serverweiten Ziel: ")
                    .append(Component.text(prettyKey(quest.targetKey())));
        };
    }

    public static Component objectiveWithProgress(Quest quest, QuestProgress progress) {
        int current = Math.min(Math.max(0, progress.getCurrentAmount()), quest.requiredAmount());
        NamedTextColor color = current >= quest.requiredAmount() ? NamedTextColor.GREEN : NamedTextColor.AQUA;
        return Component.text("Ziel: ", NamedTextColor.AQUA)
                .append(objective(quest).color(NamedTextColor.WHITE))
                .append(Component.text(" • Fortschritt: ", NamedTextColor.GRAY))
                .append(Component.text(current + "/" + quest.requiredAmount(), color));
    }

    public static Component requiredItem(Quest quest) {
        if (quest.type() != QuestType.COLLECT) return Component.empty();
        return Component.text("Benötigt: ", NamedTextColor.WHITE)
                .append(Component.text(quest.requiredAmount()))
                .append(Component.text("x "))
                .append(itemName(quest.targetKey()));
    }

    public static String requiredItemPlain(Quest quest) {
        if (quest.type() != QuestType.COLLECT) return "";
        return quest.requiredAmount() + "x " + itemNamePlain(quest.targetKey());
    }

    /** Resolves a quest item to the actual PixelRPG display name or the vanilla translated item component. */
    public static Component itemName(String key) {
        ItemDefinition definition = findDefinition(key);
        if (definition != null) return Component.text(definition.name());

        Material material = Material.matchMaterial(key == null ? "" : key.trim());
        if (material != null && material.isItem()) return Component.translatable(material.translationKey());
        return Component.text(prettyKey(key));
    }

    /** Resolves a quest item to a plain display name for string-only contexts. */
    public static String itemNamePlain(String key) {
        ItemDefinition definition = findDefinition(key);
        if (definition != null) return definition.name();

        Material material = Material.matchMaterial(key == null ? "" : key.trim());
        if (material != null && material.isItem()) return prettyKey(material.name());
        return prettyKey(key);
    }

    public static Component entityName(String key) {
        try {
            EntityType type = EntityType.valueOf(key.toUpperCase(Locale.ROOT));
            return Component.translatable(type.translationKey());
        } catch (IllegalArgumentException exception) {
            return Component.text(prettyKey(key));
        }
    }

    public static String entityNamePlain(String key) {
        try {
            EntityType type = EntityType.valueOf(key.toUpperCase(Locale.ROOT));
            return prettyKey(type.name());
        } catch (IllegalArgumentException exception) {
            return prettyKey(key);
        }
    }

    private static ItemDefinition findDefinition(String key) {
        if (key == null || key.isBlank()) return null;
        try {
            return new ItemDefinitionRegistry(de.pixelrpg.rpg.PixelRPGPlugin.getInstance()).find(key).orElse(null);
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
