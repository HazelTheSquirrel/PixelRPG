package de.pixelrpg.rpg.player;

import de.pixelrpg.rpg.profession.Profession;
import org.bukkit.configuration.file.YamlConfiguration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

public final class YamlPlayerProfileRepository implements PlayerProfileRepository {
    private final Path directory;
    private final Executor io;

    public YamlPlayerProfileRepository(Path directory, Executor io) {
        this.directory = directory;
        this.io = io;
    }

    @Override
    public CompletableFuture<PlayerProfile> load(UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Files.createDirectories(directory);
                Path file = directory.resolve(id + ".yml");
                PlayerProfile profile = new PlayerProfile(id);
                if (Files.notExists(file)) return profile;

                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file.toFile());
                profile.registered(yaml.getBoolean("registered"));
                profile.experience(yaml.getLong("experience"));
                profile.moneyMinorUnits(yaml.getLong("money-minor-units"));
                profile.storyChapter(yaml.getInt("story-chapter", -1));

                var professions = yaml.getConfigurationSection("professions");
                if (professions != null) {
                    for (Profession profession : Profession.values()) {
                        String key = profession.name().toLowerCase(Locale.ROOT);
                        profile.setProfessionLevel(profession,
                                yaml.getInt("professions." + key + ".level", Profession.MIN_LEVEL));
                        profile.setProfessionExperience(profession,
                                yaml.getLong("professions." + key + ".experience", 0L));
                        if (yaml.getBoolean("professions." + key + ".learned", false)) {
                            profile.learnProfession(profession);
                        }
                    }
                }
                for (String recipe : yaml.getStringList("unlocked-recipes")) profile.unlockRecipe(recipe);
                for (String waypoint : yaml.getStringList("unlocked-waypoints")) profile.unlockWaypoint(waypoint);
                for (String quest : yaml.getStringList("completed-quests")) profile.markQuestCompleted(quest);

                profile.revision(yaml.getLong("revision", 0L));
                profile.markClean();
                return profile;
            } catch (Exception exception) {
                throw new CompletionException(exception);
            }
        }, io);
    }

    @Override
    public CompletableFuture<Void> save(PlayerProfile profile) {
        return CompletableFuture.runAsync(() -> {
            try {
                Files.createDirectories(directory);
                YamlConfiguration yaml = new YamlConfiguration();
                yaml.set("registered", profile.registered());
                yaml.set("experience", profile.experience());
                yaml.set("money-minor-units", profile.moneyMinorUnits());
                yaml.set("story-chapter", profile.storyChapter());

                for (Profession profession : Profession.values()) {
                    String key = profession.name().toLowerCase(Locale.ROOT);
                    yaml.set("professions." + key + ".level", profile.getProfessionLevel(profession));
                    yaml.set("professions." + key + ".experience", profile.getProfessionExperience(profession));
                    yaml.set("professions." + key + ".learned", profile.hasLearnedProfession(profession));
                }
                yaml.set("unlocked-recipes", new ArrayList<>(profile.getUnlockedRecipes()));
                yaml.set("unlocked-waypoints", new ArrayList<>(profile.getUnlockedWaypoints()));
                yaml.set("completed-quests", new ArrayList<>(profile.getCompletedQuests()));
                yaml.set("revision", profile.revision());

                Path target = directory.resolve(profile.uniqueId() + ".yml");
                Path temporary = directory.resolve(profile.uniqueId() + ".yml.tmp");
                yaml.save(temporary.toFile());
                try {
                    Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
                    Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception exception) {
                throw new CompletionException(exception);
            }
        }, io);
    }
}
