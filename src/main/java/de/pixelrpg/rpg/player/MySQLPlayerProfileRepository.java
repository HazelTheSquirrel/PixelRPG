// src/main/java/de/pixelrpg/rpg/player/MySQLPlayerProfileRepository.java (VOLLSTÄNDIG, ersetzt alte Datei — attr_elytra + party_hud_enabled)
package de.pixelrpg.rpg.player;

import de.pixelrpg.rpg.leaderboard.LeaderboardEntry;
import de.pixelrpg.rpg.quest.QuestProgress;
import de.pixelrpg.rpg.storage.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class MySQLPlayerProfileRepository implements PlayerProfileRepository {

    private final DatabaseManager databaseManager;

    public MySQLPlayerProfileRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void init() throws SQLException {
        databaseManager.createTables();
    }

    @Override
    public Optional<PlayerProfile> load(UUID uuid) throws SQLException {
        String sql = "SELECT * FROM pixelrpg_players WHERE uuid = ?";
        try (Connection connection = databaseManager.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                PlayerProfile profile = new PlayerProfile(uuid);
                profile.setRegisteredInGuild(resultSet.getBoolean("registered"));
                profile.setExperience(resultSet.getLong("experience"));
                profile.setPlayerClass(parseClass(resultSet.getString("player_class")));
                profile.setMoney(resultSet.getDouble("money"));
                profile.setReceivedStartBonus(resultSet.getBoolean("start_bonus"));

                profile.setAttributePoints(PlayerAttribute.VITALITY, resultSet.getInt("attr_vitality"));
                profile.setAttributePoints(PlayerAttribute.AGILITY, resultSet.getInt("attr_agility"));
                profile.setAttributePoints(PlayerAttribute.PRECISION, resultSet.getInt("attr_precision"));
                profile.setAttributePoints(PlayerAttribute.RANGE, resultSet.getInt("attr_range"));
                profile.setAttributePoints(PlayerAttribute.TOUGHNESS, resultSet.getInt("attr_toughness"));
                profile.setAttributePoints(PlayerAttribute.SOULVIEW, resultSet.getInt("attr_soulview"));
                profile.setAttributePoints(PlayerAttribute.ELYTRA_PERMIT, resultSet.getInt("attr_elytra"));

                profile.setUnlockedWaypoints(splitCsv(resultSet.getString("waypoints")));
                profile.setStoryChapterIndex(resultSet.getInt("story_chapter"));
                profile.setCompletedQuests(splitCsv(resultSet.getString("completed_quests")));
                profile.setUnlockedAchievements(splitCsv(resultSet.getString("unlocked_achievements")));
                profile.setUnlockedTitles(splitCsv(resultSet.getString("unlocked_titles")));
                profile.setSelectedTitle(resultSet.getString("selected_title"));
                profile.setScoreboardEnabled(resultSet.getBoolean("scoreboard_enabled"));
                profile.setPartyHudEnabled(resultSet.getBoolean("party_hud_enabled"));
                profile.setPlaytimeMillis(resultSet.getLong("playtime_millis"));

                loadActiveQuests(uuid, profile);
                loadStatistics(uuid, profile);

                profile.markClean();
                return Optional.of(profile);
            }
        }
    }

    private Set<String> splitCsv(String raw) {
        Set<String> result = new HashSet<>();
        if (raw != null && !raw.isBlank()) {
            result.addAll(Arrays.asList(raw.split(",")));
        }
        return result;
    }

    private void loadActiveQuests(UUID uuid, PlayerProfile profile) throws SQLException {
        String sql = "SELECT quest_id, amount, expiry FROM pixelrpg_active_quests WHERE uuid = ?";
        try (Connection connection = databaseManager.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    profile.startQuest(new QuestProgress(
                            resultSet.getString("quest_id"),
                            resultSet.getInt("amount"),
                            resultSet.getLong("expiry")
                    ));
                }
            }
        }
    }

    private void loadStatistics(UUID uuid, PlayerProfile profile) throws SQLException {
        String sql = "SELECT stat_key, value FROM pixelrpg_player_stats WHERE uuid = ?";
        try (Connection connection = databaseManager.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    profile.setStatistic(resultSet.getString("stat_key"), resultSet.getLong("value"));
                }
            }
        }
    }

    @Override
    public void save(PlayerProfile profile) throws SQLException {
        String sql = """
                INSERT INTO pixelrpg_players
                    (uuid, registered, experience, player_class, money, start_bonus,
                     attr_vitality, attr_agility, attr_precision, attr_range, attr_toughness, attr_soulview, attr_elytra,
                     waypoints, story_chapter, completed_quests, unlocked_achievements, unlocked_titles,
                     selected_title, scoreboard_enabled, party_hud_enabled, playtime_millis)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    registered = VALUES(registered),
                    experience = VALUES(experience),
                    player_class = VALUES(player_class),
                    money = VALUES(money),
                    start_bonus = VALUES(start_bonus),
                    attr_vitality = VALUES(attr_vitality),
                    attr_agility = VALUES(attr_agility),
                    attr_precision = VALUES(attr_precision),
                    attr_range = VALUES(attr_range),
                    attr_toughness = VALUES(attr_toughness),
                    attr_soulview = VALUES(attr_soulview),
                    attr_elytra = VALUES(attr_elytra),
                    waypoints = VALUES(waypoints),
                    story_chapter = VALUES(story_chapter),
                    completed_quests = VALUES(completed_quests),
                    unlocked_achievements = VALUES(unlocked_achievements),
                    unlocked_titles = VALUES(unlocked_titles),
                    selected_title = VALUES(selected_title),
                    scoreboard_enabled = VALUES(scoreboard_enabled),
                    party_hud_enabled = VALUES(party_hud_enabled),
                    playtime_millis = VALUES(playtime_millis)
                """;
        try (Connection connection = databaseManager.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, profile.getUuid().toString());
            statement.setBoolean(2, profile.isRegisteredInGuild());
            statement.setLong(3, profile.getExperience());
            statement.setString(4, profile.getPlayerClass().name());
            statement.setDouble(5, profile.getMoney());
            statement.setBoolean(6, profile.hasReceivedStartBonus());
            statement.setInt(7, profile.getAttributePoints(PlayerAttribute.VITALITY));
            statement.setInt(8, profile.getAttributePoints(PlayerAttribute.AGILITY));
            statement.setInt(9, profile.getAttributePoints(PlayerAttribute.PRECISION));
            statement.setInt(10, profile.getAttributePoints(PlayerAttribute.RANGE));
            statement.setInt(11, profile.getAttributePoints(PlayerAttribute.TOUGHNESS));
            statement.setInt(12, profile.getAttributePoints(PlayerAttribute.SOULVIEW));
            statement.setInt(13, profile.getAttributePoints(PlayerAttribute.ELYTRA_PERMIT));
            statement.setString(14, String.join(",", profile.getUnlockedWaypoints()));
            statement.setInt(15, profile.getStoryChapterIndex());
            statement.setString(16, String.join(",", profile.getCompletedQuests()));
            statement.setString(17, String.join(",", profile.getUnlockedAchievements()));
            statement.setString(18, String.join(",", profile.getUnlockedTitles()));
            statement.setString(19, profile.getSelectedTitle());
            statement.setBoolean(20, profile.isScoreboardEnabled());
            statement.setBoolean(21, profile.isPartyHudEnabled());
            statement.setLong(22, profile.getPlaytimeMillis());
            statement.executeUpdate();
        }

        saveActiveQuests(profile);
        saveStatistics(profile);
    }

    private void saveActiveQuests(PlayerProfile profile) throws SQLException {
        String deleteSql = "DELETE FROM pixelrpg_active_quests WHERE uuid = ?";
        String insertSql = "INSERT INTO pixelrpg_active_quests (uuid, quest_id, amount, expiry) VALUES (?, ?, ?, ?)";

        try (Connection connection = databaseManager.getDataSource().getConnection()) {
            try (PreparedStatement deleteStatement = connection.prepareStatement(deleteSql)) {
                deleteStatement.setString(1, profile.getUuid().toString());
                deleteStatement.executeUpdate();
            }

            for (var progress : profile.getActiveQuests().values()) {
                try (PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
                    insertStatement.setString(1, profile.getUuid().toString());
                    insertStatement.setString(2, progress.getQuestId());
                    insertStatement.setInt(3, progress.getCurrentAmount());
                    insertStatement.setLong(4, progress.getExpiryTimestampMillis());
                    insertStatement.executeUpdate();
                }
            }
        }
    }

    private void saveStatistics(PlayerProfile profile) throws SQLException {
        String upsertSql = """
                INSERT INTO pixelrpg_player_stats (uuid, stat_key, value)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE value = VALUES(value)
                """;

        try (Connection connection = databaseManager.getDataSource().getConnection()) {
            for (var entry : profile.getAllStatistics().entrySet()) {
                try (PreparedStatement statement = connection.prepareStatement(upsertSql)) {
                    statement.setString(1, profile.getUuid().toString());
                    statement.setString(2, entry.getKey());
                    statement.setLong(3, entry.getValue());
                    statement.executeUpdate();
                }
            }
        }
    }

    @Override
    public void shutdown() {
        databaseManager.shutdown();
    }

    @Override
    public List<LeaderboardEntry> getTopByExperience(int limit) throws SQLException {
        String sql = "SELECT uuid, experience FROM pixelrpg_players ORDER BY experience DESC LIMIT ?";
        return queryLeaderboard(sql, "experience", limit);
    }

    @Override
    public List<LeaderboardEntry> getTopByMoney(int limit) throws SQLException {
        String sql = "SELECT uuid, money FROM pixelrpg_players ORDER BY money DESC LIMIT ?";
        return queryLeaderboard(sql, "money", limit);
    }

    @Override
    public List<LeaderboardEntry> getTopByStatistic(String statisticKey, int limit) throws SQLException {
        List<LeaderboardEntry> results = new ArrayList<>();
        String sql = "SELECT uuid, value FROM pixelrpg_player_stats WHERE stat_key = ? ORDER BY value DESC LIMIT ?";
        try (Connection connection = databaseManager.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, statisticKey);
            statement.setInt(2, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    results.add(new LeaderboardEntry(UUID.fromString(resultSet.getString("uuid")), resultSet.getLong("value")));
                }
            }
        }
        return results;
    }

    private List<LeaderboardEntry> queryLeaderboard(String sql, String column, int limit) throws SQLException {
        List<LeaderboardEntry> results = new ArrayList<>();
        try (Connection connection = databaseManager.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    results.add(new LeaderboardEntry(UUID.fromString(resultSet.getString("uuid")), resultSet.getDouble(column)));
                }
            }
        }
        return results;
    }

    private PlayerClass parseClass(String raw) {
        try {
            return PlayerClass.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return PlayerClass.NONE;
        }
    }
}