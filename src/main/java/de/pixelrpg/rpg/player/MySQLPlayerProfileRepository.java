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
        String sql = "SELECT * FROM pixelrpg_players WHERE uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) return null;
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
                    } else if (key.startsWith(PROFESSION_LEVEL_PREFIX)) {
                        try { profile.setProfessionLevel(Profession.valueOf(key.substring(PROFESSION_LEVEL_PREFIX.length()).toUpperCase()), (int) value); }
                        catch (IllegalArgumentException ignored) { }
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
                    (uuid, registered, experience, player_class, money, start_bonus,
                     attr_vitality, attr_agility, attr_precision, attr_range, attr_toughness, attr_soulview, attr_elytra,
                     waypoints, story_chapter, completed_quests,
                     scoreboard_enabled, party_hud_enabled, quest_tracker_enabled, playtime_millis)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    registered = VALUES(registered), experience = VALUES(experience), player_class = VALUES(player_class),
                    money = VALUES(money), start_bonus = VALUES(start_bonus), attr_vitality = VALUES(attr_vitality),
                    attr_agility = VALUES(attr_agility), attr_precision = VALUES(attr_precision), attr_range = VALUES(attr_range),
                    attr_toughness = VALUES(attr_toughness), attr_soulview = VALUES(attr_soulview), attr_elytra = VALUES(attr_elytra),
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
                    statement.setBoolean(17, profile.isScoreboardEnabled());
                    statement.setBoolean(18, profile.isPartyHudEnabled());
                    statement.setBoolean(19, profile.isQuestTrackerEnabled());
                    statement.setLong(20, profile.getPlaytimeMillis());
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

    private PlayerClass parseClass(String raw) {
        try { return PlayerClass.valueOf(raw.trim().toUpperCase()); }
        catch (IllegalArgumentException | NullPointerException e) { return PlayerClass.NONE; }
    }
}
