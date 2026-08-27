package de.pixelrpg.rpg.quest;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.item.ItemDefinition;
import de.pixelrpg.rpg.item.ItemDefinitionRegistry;
import de.pixelrpg.rpg.lang.LanguageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.Locale;

/** Centralized player-facing text for every quest screen and notification. */
public final class QuestText {
    private QuestText() {
    }

    public static Component title(Quest quest) {
        return Component.text(titlePlain(quest));
    }

    public static Component title(Player player, Quest quest) {
        return Component.text(titlePlain(player, quest));
    }

    public static String titlePlain(Quest quest) {
        String title = quest.title();
        return title == null || title.isBlank() ? "Unnamed Quest" : title;
    }

    public static String titlePlain(Player player, Quest quest) {
        String localized = localizedContent(player, quest, "title");
        return localized != null ? localized : titlePlain(quest);
    }

    public static Component description(Quest quest) {
        String description = quest.description();
        return Component.text(description == null || description.isBlank() ? "No description available." : description);
    }

    public static Component description(Player player, Quest quest) {
        String localized = localizedContent(player, quest, "description");
        if (localized != null) return Component.text(localized);
        return description(quest);
    }

    public static Component objective(Quest quest) {
        return objective(null, quest);
    }

    public static Component objective(Player player, Quest quest) {
        LanguageManager lang = languageManager();
        String amount = String.valueOf(quest.requiredAmount());
        return switch (quest.type()) {
            case HUNT -> lang.get(player, "quest.objective.hunt", "amount", amount)
                    .append(Component.text(" "))
                    .append(entityName(quest.targetKey()));
            case COLLECT -> lang.get(player, "quest.objective.collect", "amount", amount)
                    .append(Component.text(" "))
                    .append(itemName(quest.targetKey()));
            case TALK_TO_NPC -> lang.get(player, "quest.objective.talk", "target", prettyKey(quest.targetKey()));
            case REACH_LOCATION -> lang.get(player, "quest.objective.reach");
            case GLOBAL_EVENT -> lang.get(player, "quest.objective.global", "target", prettyKey(quest.targetKey()));
        };
    }

    public static Component objectiveWithProgress(Quest quest, QuestProgress progress) {
        return objectiveWithProgress(null, quest, progress);
    }

    public static Component objectiveWithProgress(Player player, Quest quest, QuestProgress progress) {
        int current = Math.min(Math.max(0, progress.getCurrentAmount()), quest.requiredAmount());
        NamedTextColor color = current >= quest.requiredAmount() ? NamedTextColor.GREEN : NamedTextColor.AQUA;
        return languageManager().get(player, "quest.objective-progress",
                        "objective", objective(player, quest).toString(),
                        "current", String.valueOf(current),
                        "required", String.valueOf(quest.requiredAmount()))
                .color(NamedTextColor.WHITE)
                .append(Component.text(" "))
                .append(Component.text(current + "/" + quest.requiredAmount(), color));
    }

    public static Component requiredItem(Quest quest) {
        return requiredItem(null, quest);
    }

    public static Component requiredItem(Player player, Quest quest) {
        if (quest.type() != QuestType.COLLECT) return Component.empty();
        return languageManager().get(player, "quest.required-item", "amount", String.valueOf(quest.requiredAmount()))
                .append(Component.text(" "))
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

    private static String localizedContent(Player player, Quest quest, String field) {
        if (player == null || quest.id() == null || quest.id().isBlank()) return null;
        LanguageManager lang = languageManager();
        String key = "quest.content." + quest.id() + "." + field;
        String value = lang.get(player, key).toString();
        return value.equals(key) ? null : value;
    }

    private static LanguageManager languageManager() {
        LanguageManager manager = PixelRPGPlugin.getInstance().getLanguageManager();
        if (manager == null) throw new IllegalStateException("LanguageManager is not initialized.");
        return manager;
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
        if (key == null || key.isBlank()) return "Unknown";
        String value = key.trim();
        int separator = value.indexOf(':');
        if (separator >= 0 && separator + 1 < value.length()) value = value.substring(separator + 1);
        value = value.toLowerCase(Locale.ROOT).replace('_', ' ').replace('-', ' ');
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
