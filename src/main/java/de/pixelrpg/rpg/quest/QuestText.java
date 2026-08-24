package de.pixelrpg.rpg.quest;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

public final class QuestText {
    private QuestText() {
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

    private static Component translatableEntity(String key) {
        try {
            return Component.translatable(EntityType.valueOf(key.toUpperCase()).translationKey());
        } catch (IllegalArgumentException exception) {
            return Component.text(key);
        }
    }

    private static Component translatableMaterial(String key) {
        try {
            return Component.translatable(Material.valueOf(key.toUpperCase()).translationKey());
        } catch (IllegalArgumentException exception) {
            return Component.text(key);
        }
    }
}
