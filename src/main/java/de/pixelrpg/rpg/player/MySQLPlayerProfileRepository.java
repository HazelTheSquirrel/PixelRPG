package de.pixelrpg.rpg.player;

import de.pixelrpg.rpg.profession.Profession;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

public final class MySQLPlayerProfileRepository implements PlayerProfileRepository {
    private static final String LEVEL_PREFIX = "profession.level.";
    private static final String XP_PREFIX = "profession.xp.";
    private static final String LEARNED_PREFIX = "profession.learned.";
    private static final String RECIPE_PREFIX = "recipe.unlocked.";
    private static final String QUEST_PREFIX = "quest.completed.";
    private static final String ACTIVE_QUEST_PREFIX = "quest.active.current.";
    private static final String ACTIVE_EXPIRY_PREFIX = "quest.active.expiry.";

    private final DataSource dataSource;
    private final Executor io;

    public MySQLPlayerProfileRepository(DataSource dataSource, Executor io) {
        this.dataSource = dataSource;
        this.io = io;
    }

    @Override
    public CompletableFuture<PlayerProfile> load(UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {
                PlayerProfile profile = loadPlayer(connection, id);
                if (profile == null) return new PlayerProfile(id);
                loadDomainState(connection, id, profile);
                profile.markClean();
                return profile;
            } catch (SQLException exception) {
                throw new CompletionException(exception);
            }
        }, io);
    }

    private PlayerProfile loadPlayer(Connection connection, UUID id) throws SQLException {
        try (var statement = connection.prepareStatement(
                "SELECT registered, experience, money_minor_units, waypoints, story_chapter, persistence_revision " +
                "FROM pixelrpg_players WHERE uuid = ?")) {
            statement.setString(1, id.toString());
            try (var result = statement.executeQuery()) {
                if (!result.next()) return null;
                PlayerProfile profile = new PlayerProfile(id);
                profile.registered(result.getBoolean(1));
                profile.experience(result.getLong(2));
                profile.moneyMinorUnits(result.getLong(3));
                String waypoints = result.getString(4);
                if (waypoints != null && !waypoints.isBlank()) for (String waypoint : waypoints.split(",")) profile.unlockWaypoint(waypoint);
                profile.storyChapter(result.getInt(5));
                profile.revision(result.getLong(6));
                return profile;
            }
        }
    }

    private void loadDomainState(Connection connection, UUID id, PlayerProfile profile) throws SQLException {
        java.util.Map<String, Integer> activeCurrent = new java.util.HashMap<>();
        java.util.Map<String, Long> activeExpiry = new java.util.HashMap<>();
        try (var statement = connection.prepareStatement(
                "SELECT stat_key, value FROM pixelrpg_player_stats WHERE uuid = ?")) {
            statement.setString(1, id.toString());
            try (var result = statement.executeQuery()) {
                while (result.next()) {
                    String key = result.getString(1);
                    long value = result.getLong(2);
                    if (key.startsWith(LEVEL_PREFIX)) {
                        Profession profession = profession(key.substring(LEVEL_PREFIX.length()));
                        if (profession != null) profile.setProfessionLevel(profession, (int) value);
                    } else if (key.startsWith(XP_PREFIX)) {
                        Profession profession = profession(key.substring(XP_PREFIX.length()));
                        if (profession != null) profile.setProfessionExperience(profession, value);
                    } else if (key.startsWith(LEARNED_PREFIX)) {
                        Profession profession = profession(key.substring(LEARNED_PREFIX.length()));
                        if (profession != null && value > 0L) profile.learnProfession(profession);
                    } else if (key.startsWith(RECIPE_PREFIX) && value > 0L) {
                        profile.unlockRecipe(key.substring(RECIPE_PREFIX.length()));
                    } else if (key.startsWith(QUEST_PREFIX) && value > 0L) {
                        profile.markQuestCompleted(key.substring(QUEST_PREFIX.length()));
                    } else if (key.startsWith(ACTIVE_QUEST_PREFIX)) {
                        activeCurrent.put(key.substring(ACTIVE_QUEST_PREFIX.length()), (int) value);
                    } else if (key.startsWith(ACTIVE_EXPIRY_PREFIX)) {
                        activeExpiry.put(key.substring(ACTIVE_EXPIRY_PREFIX.length()), value);
                    }
                }
            }
        }
        for (String questId : activeCurrent.keySet()) {
            profile.startQuest(new de.pixelrpg.rpg.quest.QuestProgress(
                    questId, activeCurrent.getOrDefault(questId, 0), activeExpiry.getOrDefault(questId, 0L)));
        }
    }

    private static Profession profession(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Profession.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT)); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    @Override
    public CompletableFuture<Void> save(PlayerProfile profile) {
        return CompletableFuture.runAsync(() -> {
            long nextRevision = profile.revision();
            try (Connection connection = dataSource.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    upsertPlayer(connection, profile, nextRevision);
                    replaceDomainState(connection, profile);
                    connection.commit();
                } catch (SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(true);
                }
            } catch (SQLException exception) {
                throw new CompletionException(exception);
            }
        }, io);
    }

    private void upsertPlayer(Connection connection, PlayerProfile profile, long revision) throws SQLException {
        try (var statement = connection.prepareStatement(
                "INSERT INTO pixelrpg_players " +
                "(uuid, registered, experience, money_minor_units, story_chapter, persistence_revision) " +
                "VALUES (?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE registered=VALUES(registered), experience=VALUES(experience), " +
                "money_minor_units=VALUES(money_minor_units), story_chapter=VALUES(story_chapter), " +
                "persistence_revision=VALUES(persistence_revision)")) {
            statement.setString(1, profile.uniqueId().toString());
            statement.setBoolean(2, profile.registered());
            statement.setLong(3, profile.experience());
            statement.setLong(4, profile.moneyMinorUnits());
            statement.setString(5, String.join(",", profile.getUnlockedWaypoints()));
            statement.setInt(6, profile.storyChapter());
            statement.setLong(7, revision);
            statement.executeUpdate();
        }
    }

    private void replaceDomainState(Connection connection, PlayerProfile profile) throws SQLException {
        try (var delete = connection.prepareStatement("DELETE FROM pixelrpg_player_stats WHERE uuid = ?")) {
            delete.setString(1, profile.uniqueId().toString());
            delete.executeUpdate();
        }

        try (var insert = connection.prepareStatement(
                "INSERT INTO pixelrpg_player_stats (uuid, stat_key, value) VALUES (?, ?, ?)")) {
            for (Profession profession : Profession.values()) {
                insert.setString(1, profile.uniqueId().toString());
                insert.setString(2, LEVEL_PREFIX + profession.name().toLowerCase(java.util.Locale.ROOT));
                insert.setLong(3, profile.getProfessionLevel(profession));
                insert.addBatch();

                insert.setString(2, XP_PREFIX + profession.name().toLowerCase(java.util.Locale.ROOT));
                insert.setLong(3, profile.getProfessionExperience(profession));
                insert.addBatch();

                insert.setString(2, LEARNED_PREFIX + profession.name().toLowerCase(java.util.Locale.ROOT));
                insert.setLong(3, profile.hasLearnedProfession(profession) ? 1L : 0L);
                insert.addBatch();
            }
            for (String recipe : profile.getUnlockedRecipes()) {
                insert.setString(2, RECIPE_PREFIX + recipe);
                insert.setLong(3, 1L);
                insert.addBatch();
            }
            for (String quest : profile.getCompletedQuests()) {
                insert.setString(2, QUEST_PREFIX + quest);
                insert.setLong(3, 1L);
                insert.addBatch();
            }
            for (var entry : profile.getActiveQuests().entrySet()) {
                insert.setString(2, ACTIVE_QUEST_PREFIX + entry.getKey());
                insert.setLong(3, entry.getValue().getCurrentAmount());
                insert.addBatch();
                insert.setString(2, ACTIVE_EXPIRY_PREFIX + entry.getKey());
                insert.setLong(3, entry.getValue().getExpiryTimestampMillis());
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }
}
