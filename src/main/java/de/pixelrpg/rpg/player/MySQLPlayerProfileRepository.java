package de.pixelrpg.rpg.player;

import de.pixelrpg.rpg.profession.Profession;
import de.pixelrpg.rpg.quest.QuestProgress;
import de.pixelrpg.rpg.storage.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class MySQLPlayerProfileRepository implements PlayerProfileRepository {
    private static final String PROFESSION_LEVEL_PREFIX = "profession.";
    private static final String PROFESSION_XP_PREFIX = "profession.xp.";
    private static final String PROFESSION_LEARNED_PREFIX = "profession.learned.";
    private static final String RECIPE_UNLOCK_PREFIX = "recipe.unlocked.";
    private final DatabaseManager databaseManager;

    public MySQLPlayerProfileRepository(DatabaseManager databaseManager) { this.databaseManager = databaseManager; }

    @Override
    public void init() throws SQLException { databaseManager.createTables(); }

    @Override
    public Optional<PlayerProfile> load(UUID uuid) throws SQLException {
        try (Connection connection = databaseManager.getDataSource().getConnection()) {
            PlayerProfile profile = loadPlayerRow(connection, uuid);
            if (profile == null) return Optional.empty();
            loadActiveQuests(connection, uuid, profile);
            loadStatistics(connection, uuid, profile);
            profile.markClean();
            return Optional.of(profile);
        }
    }

    private PlayerProfile loadPlayerRow(Connection connection, UUID uuid) throws SQLException {
        String sql = "SELECT uuid, registered, experience, money, waypoints, story_chapter, completed_quests, scoreboard_enabled, party_hud_enabled, quest_tracker_enabled, playtime_millis FROM pixelrpg_players WHERE uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) return null;
                PlayerProfile profile = new PlayerProfile(uuid);
                profile.setRegisteredInGuild(resultSet.getBoolean("registered"));
                profile.setExperience(resultSet.getLong("experience"));
                profile.setMoney(resultSet.getDouble("money"));
                profile.setUnlockedWaypoints(splitCsv(resultSet.getString("waypoints")));
                profile.setStoryChapterIndex(resultSet.getInt("story_chapter"));
                profile.setCompletedQuests(splitCsv(resultSet.getString("completed_quests")));
                profile.setScoreboardEnabled(resultSet.getBoolean("scoreboard_enabled"));
                profile.setPartyHudEnabled(resultSet.getBoolean("party_hud_enabled"));
                profile.setQuestTrackerEnabled(resultSet.getBoolean("quest_tracker_enabled"));
                profile.setPlaytimeMillis(resultSet.getLong("playtime_millis"));
                return profile;
            }
        }
    }

    private Set<String> splitCsv(String raw) {
        Set<String> result = new HashSet<>();
        if (raw != null && !raw.isBlank()) result.addAll(Arrays.asList(raw.split(",")));
        return result;
    }

    private void loadActiveQuests(Connection connection, UUID uuid, PlayerProfile profile) throws SQLException {
        String sql = "SELECT quest_id, amount, expiry FROM pixelrpg_active_quests WHERE uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) profile.startQuest(new QuestProgress(resultSet.getString("quest_id"), resultSet.getInt("amount"), resultSet.getLong("expiry")));
            }
        }
    }

    private void loadStatistics(Connection connection, UUID uuid, PlayerProfile profile) throws SQLException {
        String sql = "SELECT stat_key, value FROM pixelrpg_player_stats WHERE uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String key = resultSet.getString("stat_key");
                    long value = resultSet.getLong("value");
                    if (key.startsWith(PROFESSION_XP_PREFIX)) {
                        try { profile.setProfessionExperience(Profession.valueOf(key.substring(PROFESSION_XP_PREFIX.length()).toUpperCase()), value); }
                        catch (IllegalArgumentException ignored) { }
                    } else if (key.startsWith(PROFESSION_LEARNED_PREFIX)) {
                        try { if (value > 0L) profile.learnProfession(Profession.valueOf(key.substring(PROFESSION_LEARNED_PREFIX.length()).toUpperCase())); }
                        catch (IllegalArgumentException ignored) { }
                    } else if (key.startsWith(PROFESSION_LEVEL_PREFIX)) {
                        try { profile.setProfessionLevel(Profession.valueOf(key.substring(PROFESSION_LEVEL_PREFIX.length()).toUpperCase()), (int) value); }
                        catch (IllegalArgumentException ignored) { }
                    } else if (key.startsWith(RECIPE_UNLOCK_PREFIX)) {
                        if (value > 0L) profile.unlockRecipe(key.substring(RECIPE_UNLOCK_PREFIX.length()));
                    } else {
                        profile.setStatistic(key, value);
                    }
                }
            }
        }
    }

    @Override
    public void save(PlayerProfile profile) throws SQLException {
        String upsertPlayerSql = """
                INSERT INTO pixelrpg_players
                    (uuid, registered, experience, money, waypoints, story_chapter, completed_quests,
                     scoreboard_enabled, party_hud_enabled, quest_tracker_enabled, playtime_millis)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    registered = VALUES(registered), experience = VALUES(experience), money = VALUES(money),
                    waypoints = VALUES(waypoints), story_chapter = VALUES(story_chapter), completed_quests = VALUES(completed_quests),
                    scoreboard_enabled = VALUES(scoreboard_enabled), party_hud_enabled = VALUES(party_hud_enabled),
                    quest_tracker_enabled = VALUES(quest_tracker_enabled), playtime_millis = VALUES(playtime_millis)
                """;
        String deleteQuestsSql = "DELETE FROM pixelrpg_active_quests WHERE uuid = ?";
        String insertQuestSql = "INSERT INTO pixelrpg_active_quests (uuid, quest_id, amount, expiry) VALUES (?, ?, ?, ?)";
        String upsertStatSql = """
                INSERT INTO pixelrpg_player_stats (uuid, stat_key, value) VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE value = VALUES(value)
                """;

        try (Connection connection = databaseManager.getDataSource().getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(upsertPlayerSql)) {
                    statement.setString(1, profile.getUuid().toString());
                    statement.setBoolean(2, profile.isRegisteredInGuild());
                    statement.setLong(3, profile.getExperience());
                    statement.setDouble(4, profile.getMoney());
                    statement.setString(5, String.join(",", profile.getUnlockedWaypoints()));
                    statement.setInt(6, profile.getStoryChapterIndex());
                    statement.setString(7, String.join(",", profile.getCompletedQuests()));
                    statement.setBoolean(8, profile.isScoreboardEnabled());
                    statement.setBoolean(9, profile.isPartyHudEnabled());
                    statement.setBoolean(10, profile.isQuestTrackerEnabled());
                    statement.setLong(11, profile.getPlaytimeMillis());
                    statement.executeUpdate();
                }
                try (PreparedStatement deleteStatement = connection.prepareStatement(deleteQuestsSql)) {
                    deleteStatement.setString(1, profile.getUuid().toString());
                    deleteStatement.executeUpdate();
                }
                if (!profile.getActiveQuests().isEmpty()) {
                    try (PreparedStatement insertStatement = connection.prepareStatement(insertQuestSql)) {
                        for (QuestProgress progress : profile.getActiveQuests().values()) {
                            insertStatement.setString(1, profile.getUuid().toString());
                            insertStatement.setString(2, progress.getQuestId());
                            insertStatement.setInt(3, progress.getCurrentAmount());
                            insertStatement.setLong(4, progress.getExpiryTimestampMillis());
                            insertStatement.addBatch();
                        }
                        insertStatement.executeBatch();
                    }
                }
                try (PreparedStatement statStatement = connection.prepareStatement(upsertStatSql)) {
                    for (var entry : profile.getAllStatistics().entrySet()) {
                        statStatement.setString(1, profile.getUuid().toString());
                        statStatement.setString(2, entry.getKey());
                        statStatement.setLong(3, entry.getValue());
                        statStatement.addBatch();
                    }
                    for (Profession profession : Profession.values()) {
                        statStatement.setString(1, profile.getUuid().toString());
                        statStatement.setString(2, PROFESSION_LEVEL_PREFIX + profession.name().toLowerCase());
                        statStatement.setLong(3, profile.getProfessionLevel(profession));
                        statStatement.addBatch();
                        statStatement.setString(2, PROFESSION_XP_PREFIX + profession.name().toLowerCase());
                        statStatement.setLong(3, profile.getProfessionExperience(profession));
                        statStatement.addBatch();
                        statStatement.setString(2, PROFESSION_LEARNED_PREFIX + profession.name().toLowerCase());
                        statStatement.setLong(3, profile.hasLearnedProfession(profession) ? 1L : 0L);
                        statStatement.addBatch();
                    }
                    for (String recipeId : profile.getUnlockedRecipes()) {
                        statStatement.setString(1, profile.getUuid().toString());
                        statStatement.setString(2, RECIPE_UNLOCK_PREFIX + recipeId);
                        statStatement.setLong(3, 1L);
                        statStatement.addBatch();
                    }
                    statStatement.executeBatch();
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    @Override public void shutdown() { databaseManager.shutdown(); }
}
