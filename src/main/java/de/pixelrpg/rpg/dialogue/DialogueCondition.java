package de.pixelrpg.rpg.dialogue;

import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.function.Predicate;

@FunctionalInterface
public interface DialogueCondition {
    boolean test(Player player);

    default boolean test(DialogueContext context) {
        return test(context.player());
    }

    static DialogueCondition always() {
        return player -> true;
    }

    static DialogueCondition of(Predicate<Player> predicate) {
        return Objects.requireNonNull(predicate, "predicate")::test;
    }
}
