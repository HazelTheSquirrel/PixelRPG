package de.pixelrpg.rpg.quest;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

public final class QuestText {
    private QuestText() {
    }

    public static Component title(Quest quest) {
        String title = quest.title();
        return Component.text(title == null || title.isBlank() ? quest.id() : title);
    }

    public static Component objective(Quest quest) {
        return switch (quest.type()) {
            case HUNT -> Component.text("Töte ")
                    .append(Component.text(quest.requiredAmount()))
                    .append(Component.text(" "))
                    .append(translatableEntity(quest.targetKey()));
            case COLLECT -> Component.text("Sammle ")
                    .append(Component.text(quest.requiredAmount()))
                    .append(Component.text(" "))
                    .append(translatableMaterial(quest.targetKey()));
            case TALK_TO_NPC -> Component.text("Sprich mit NPC: ").append(Component.text(quest.targetKey()));
            case REACH_LOCATION -> Component.text("Erreiche den angegebenen Zielort.");
            case GLOBAL_EVENT -> Component.text("Beteilige dich am serverweiten Ziel: ").append(Component.text(quest.targetKey()));
        };
    }

    public static Component requiredItem(Quest quest) {
        if (quest.type() != QuestType.COLLECT) return Component.empty();
        return Component.text("Benötigt: ")
                .append(Component.text(quest.requiredAmount()))
                .append(Component.text("x "))
                .append(translatableMaterial(quest.targetKey()));
    }

    public static String requiredItemPlain(Quest quest) {
        if (quest.type() != QuestType.COLLECT) return "";
        return quest.requiredAmount() + "x " + prettyMaterial(quest.targetKey());
    }

    private static Component translatableEntity(String key) {
        try {
            return Component.translatable(EntityType.valueOf(key.toUpperCase()).translationKey());
        } catch (IllegalArgumentException exception) {
            return Component.text(prettyMaterial(key));
        }
    }

    private static Component translatableMaterial(String key) {
        try {
            return Component.translatable(Material.valueOf(key.toUpperCase()).translationKey());
        } catch (IllegalArgumentException exception) {
            return Component.text(prettyMaterial(key));
        }
    }

    private static String prettyMaterial(String key) {
        if (key == null || key.isBlank()) return "Unbekannt";
        String value = key.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
