package de.pixelrpg.rpg.player;

import de.pixelrpg.rpg.equipment.EquipmentSlot;
import de.pixelrpg.rpg.profession.Profession;
import de.pixelrpg.rpg.quest.QuestProgress;
import de.pixelrpg.rpg.storage.DatabaseManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class MySQLPlayerProfileRepository implements PlayerProfileRepository {
    private static final String PROFESSION_LEVEL_PREFIX = "profession."; private static final String PROFESSION_XP_PREFIX = "profession.xp."; private static final String PROFESSION_LEARNED_PREFIX = "profession.learned."; private static final String RECIPE_UNLOCK_PREFIX = "recipe.unlocked.";
    private final DatabaseManager databaseManager;
    public MySQLPlayerProfileRepository(DatabaseManager databaseManager) { this.databaseManager = databaseManager; }
    @Override public void init() throws SQLException { databaseManager.createTables(); }
    @Override public Optional<PlayerProfile> load(UUID uuid) throws SQLException { try (Connection connection = databaseManager.getDataSource().getConnection()) { PlayerProfile profile = loadPlayerRow(connection, uuid); if (profile == null) return Optional.empty(); loadActiveQuests(connection, uuid, profile); loadStatistics(connection, uuid, profile); loadEquipment(connection, uuid, profile); loadCompanions(connection, uuid, profile); profile.markClean(); return Optional.of(profile); } }
    private PlayerProfile loadPlayerRow(Connection connection, UUID uuid) throws SQLException { String sql = "SELECT uuid, registered, experience, money_minor_units, waypoints, story_chapter, completed_quests, scoreboard_enabled, party_hud_enabled, quest_tracker_enabled, playtime_millis, persistence_revision FROM pixelrpg_players WHERE uuid = ?"; try (PreparedStatement statement = connection.prepareStatement(sql)) { statement.setString(1, uuid.toString()); try (ResultSet resultSet = statement.executeQuery()) { if (!resultSet.next()) return null; PlayerProfile profile = new PlayerProfile(uuid); profile.setRegistered(resultSet.getBoolean("registered")); profile.setExperience(resultSet.getLong("experience")); profile.setMoneyMinorUnits(resultSet.getLong("money_minor_units")); profile.setUnlockedWaypoints(splitCsv(resultSet.getString("waypoints"))); profile.setStoryChapterIndex(resultSet.getInt("story_chapter")); profile.setCompletedQuests(splitCsv(resultSet.getString("completed_quests"))); profile.setScoreboardEnabled(resultSet.getBoolean("scoreboard_enabled")); profile.setPartyHudEnabled(resultSet.getBoolean("party_hud_enabled")); profile.setQuestTrackerEnabled(resultSet.getBoolean("quest_tracker_enabled")); profile.setPlaytimeMillis(resultSet.getLong("playtime_millis")); profile.setPersistenceRevision(resultSet.getLong("persistence_revision")); return profile; } } }
    private Set<String> splitCsv(String raw) { Set<String> result = new HashSet<>(); if (raw != null && !raw.isBlank()) result.addAll(Arrays.asList(raw.split(","))); return result; }
    private void loadActiveQuests(Connection connection, UUID uuid, PlayerProfile profile) throws SQLException { String sql = "SELECT quest_id, amount, expiry FROM pixelrpg_active_quests WHERE uuid = ?"; try (PreparedStatement statement = connection.prepareStatement(sql)) { statement.setString(1, uuid.toString()); try (ResultSet resultSet = statement.executeQuery()) { while (resultSet.next()) profile.startQuest(new QuestProgress(resultSet.getString("quest_id"), resultSet.getInt("amount"), resultSet.getLong("expiry"))); } } }
    private void loadStatistics(Connection connection, UUID uuid, PlayerProfile profile) throws SQLException { String sql = "SELECT stat_key, value FROM pixelrpg_player_stats WHERE uuid = ?"; try (PreparedStatement statement = connection.prepareStatement(sql)) { statement.setString(1, uuid.toString()); try (ResultSet resultSet = statement.executeQuery()) { while (resultSet.next()) { String key = resultSet.getString("stat_key"); long value = resultSet.getLong("value"); if (key.startsWith(PROFESSION_XP_PREFIX)) { Profession profession = parseProfessionKey(key.substring(PROFESSION_XP_PREFIX.length())); if (profession != null) profile.setProfessionExperience(profession, value); } else if (key.startsWith(PROFESSION_LEARNED_PREFIX)) { Profession profession = parseProfessionKey(key.substring(PROFESSION_LEARNED_PREFIX.length())); if (profession != null && value > 0L) profile.learnProfession(profession); } else if (key.startsWith(PROFESSION_LEVEL_PREFIX)) { Profession profession = parseProfessionKey(key.substring(PROFESSION_LEVEL_PREFIX.length())); if (profession != null) profile.setProfessionLevel(profession, (int) value); } else if (key.startsWith(RECIPE_UNLOCK_PREFIX)) { if (value > 0L) profile.unlockRecipe(key.substring(RECIPE_UNLOCK_PREFIX.length())); } else profile.setStatistic(key, value); } } } }
    private Profession parseProfessionKey(String raw) { if (raw == null || raw.isBlank()) return null; String normalized = raw.trim().toUpperCase(); if (normalized.equals("PROVISIONER")) return Profession.COOK; try { return Profession.valueOf(normalized); } catch (IllegalArgumentException ignored) { return null; } }
    private void loadEquipment(Connection connection, UUID uuid, PlayerProfile profile) throws SQLException { MapBuilder equipment = new MapBuilder(); String sql = "SELECT slot, item_yaml FROM pixelrpg_player_equipment WHERE uuid = ?"; try (PreparedStatement statement = connection.prepareStatement(sql)) { statement.setString(1, uuid.toString()); try (ResultSet resultSet = statement.executeQuery()) { while (resultSet.next()) try { EquipmentSlot slot = EquipmentSlot.valueOf(resultSet.getString("slot")); YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new StringReader(resultSet.getString("item_yaml"))); ItemStack item = yaml.getItemStack("item"); if (item != null && !item.isEmpty()) equipment.put(slot, item); } catch (IllegalArgumentException ignored) { } } } profile.setEquipment(equipment.values()); }
    private void loadCompanions(Connection connection, UUID uuid, PlayerProfile profile) throws SQLException {
        String sql = "SELECT companion_id,name,level,experience,unlocked,active,skin_value,skin_signature,helmet_yaml,chestplate_yaml,leggings_yaml,boots_yaml,main_hand_yaml,off_hand_yaml FROM pixelrpg_player_companions WHERE uuid = ?";
        Map<String, CompanionState> companions = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Map<String, ItemStack> items = new LinkedHashMap<>();
                    putItem(items, "helmet", resultSet.getString("helmet_yaml"));
                    putItem(items, "chestplate", resultSet.getString("chestplate_yaml"));
                    putItem(items, "leggings", resultSet.getString("leggings_yaml"));
                    putItem(items, "boots", resultSet.getString("boots_yaml"));
                    putItem(items, "main-hand", resultSet.getString("main_hand_yaml"));
                    putItem(items, "off-hand", resultSet.getString("off_hand_yaml"));
                    companions.put(resultSet.getString("companion_id"), new CompanionState(
                            resultSet.getString("companion_id"), resultSet.getString("name"),
                            Math.max(1, resultSet.getInt("level")), Math.max(0L, resultSet.getLong("experience")),
                            resultSet.getBoolean("unlocked"), resultSet.getBoolean("active"), items,
                            resultSet.getString("skin_value"), resultSet.getString("skin_signature")));
                }
            }
        }
        profile.setCompanions(companions);
    }
    private static void putItem(Map<String, ItemStack> items, String slot, String yamlText) {
        if (yamlText == null || yamlText.isBlank()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new StringReader(yamlText));
        ItemStack item = yaml.getItemStack("item");
        if (item != null && !item.isEmpty()) items.put(slot, item);
    }
    private static String itemYaml(ItemStack item) {
        if (item == null || item.isEmpty()) return null;
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("item", item);
        return yaml.saveToString();
    }
    @Override public long save(PlayerProfile profile) throws SQLException {
        String upsertPlayerSql = "INSERT INTO pixelrpg_players (uuid, registered, experience, money_minor_units, waypoints, story_chapter, completed_quests, scoreboard_enabled, party_hud_enabled, quest_tracker_enabled, playtime_millis, persistence_revision) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE registered = VALUES(registered), experience = VALUES(experience), money_minor_units = VALUES(money_minor_units), waypoints = VALUES(waypoints), story_chapter = VALUES(story_chapter), completed_quests = VALUES(completed_quests), scoreboard_enabled = VALUES(scoreboard_enabled), party_hud_enabled = VALUES(party_hud_enabled), quest_tracker_enabled = VALUES(quest_tracker_enabled), playtime_millis = VALUES(playtime_millis), persistence_revision = VALUES(persistence_revision)";
        String lockRevisionSql = "SELECT persistence_revision FROM pixelrpg_players WHERE uuid = ? FOR UPDATE"; String deleteQuestsSql = "DELETE FROM pixelrpg_active_quests WHERE uuid = ?"; String insertQuestSql = "INSERT INTO pixelrpg_active_quests (uuid, quest_id, amount, expiry) VALUES (?, ?, ?, ?)"; String deleteEquipmentSql = "DELETE FROM pixelrpg_player_equipment WHERE uuid = ?"; String insertEquipmentSql = "INSERT INTO pixelrpg_player_equipment (uuid, slot, item_yaml) VALUES (?, ?, ?)"; String deleteStatsSql = "DELETE FROM pixelrpg_player_stats WHERE uuid = ?"; String deleteCompanionsSql = "DELETE FROM pixelrpg_player_companions WHERE uuid = ?"; String insertCompanionSql = "INSERT INTO pixelrpg_player_companions (uuid,companion_id,name,level,experience,unlocked,active,skin_value,skin_signature,helmet_yaml,chestplate_yaml,leggings_yaml,boots_yaml,main_hand_yaml,off_hand_yaml) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"; String upsertStatSql = "INSERT INTO pixelrpg_player_stats (uuid, stat_key, value) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE value = VALUES(value)";
        long expectedRevision = profile.getPersistenceRevision(); long nextRevision = expectedRevision + 1L; if (nextRevision < 0L) throw new SQLException("Player persistence revision overflow for " + profile.getUuid());
        Set<String> unlockedWaypoints = profile.getUnlockedWaypoints();
        Set<String> completedQuests = profile.getCompletedQuests();
        Set<String> unlockedRecipes = profile.getUnlockedRecipes();
        Map<String, QuestProgress> activeQuests = profile.getActiveQuests();
        Map<EquipmentSlot, ItemStack> equipment = profile.getEquipment();
        Map<String, Long> statistics = profile.getAllStatistics();
        Map<String, CompanionState> companions = profile.getCompanions();
        try (Connection connection = databaseManager.getDataSource().getConnection()) { connection.setAutoCommit(false); try { Long databaseRevision = null; try (PreparedStatement statement = connection.prepareStatement(lockRevisionSql)) { statement.setString(1, profile.getUuid().toString()); try (ResultSet resultSet = statement.executeQuery()) { if (resultSet.next()) databaseRevision = resultSet.getLong(1); } } if (databaseRevision != null && databaseRevision.longValue() != expectedRevision) throw new SQLException("Stale player profile revision for " + profile.getUuid() + ": memory=" + expectedRevision + ", database=" + databaseRevision); if (databaseRevision == null && expectedRevision != 0L) throw new SQLException("Player profile revision " + expectedRevision + " exists in memory but the database row is missing for " + profile.getUuid());
            try (PreparedStatement statement = connection.prepareStatement(upsertPlayerSql)) { statement.setString(1, profile.getUuid().toString()); statement.setBoolean(2, profile.isRegistered()); statement.setLong(3, profile.getExperience()); statement.setLong(4, profile.getMoneyMinorUnits()); statement.setString(5, String.join(",", unlockedWaypoints)); statement.setInt(6, profile.getStoryChapterIndex()); statement.setString(7, String.join(",", completedQuests)); statement.setBoolean(8, profile.isScoreboardEnabled()); statement.setBoolean(9, profile.isPartyHudEnabled()); statement.setBoolean(10, profile.isQuestTrackerEnabled()); statement.setLong(11, profile.getPlaytimeMillis()); statement.setLong(12, nextRevision); statement.executeUpdate(); }
            try (PreparedStatement statement = connection.prepareStatement(deleteQuestsSql)) { statement.setString(1, profile.getUuid().toString()); statement.executeUpdate(); } if (!activeQuests.isEmpty()) try (PreparedStatement statement = connection.prepareStatement(insertQuestSql)) { for (QuestProgress progress : activeQuests.values()) { statement.setString(1, profile.getUuid().toString()); statement.setString(2, progress.getQuestId()); statement.setInt(3, progress.getCurrentAmount()); statement.setLong(4, progress.getExpiryTimestampMillis()); statement.addBatch(); } statement.executeBatch(); }
            try (PreparedStatement statement = connection.prepareStatement(deleteEquipmentSql)) { statement.setString(1, profile.getUuid().toString()); statement.executeUpdate(); } if (!equipment.isEmpty()) try (PreparedStatement statement = connection.prepareStatement(insertEquipmentSql)) { for (var entry : equipment.entrySet()) { YamlConfiguration yaml = new YamlConfiguration(); yaml.set("item", entry.getValue()); statement.setString(1, profile.getUuid().toString()); statement.setString(2, entry.getKey().name()); statement.setString(3, yaml.saveToString()); statement.addBatch(); } statement.executeBatch(); }
            try (PreparedStatement statement = connection.prepareStatement(deleteCompanionsSql)) { statement.setString(1, profile.getUuid().toString()); statement.executeUpdate(); }
            if (!companions.isEmpty()) try (PreparedStatement statement = connection.prepareStatement(insertCompanionSql)) {
                for (CompanionState companion : companions.values()) {
                    statement.setString(1, profile.getUuid().toString()); statement.setString(2, companion.id()); statement.setString(3, companion.name());
                    statement.setInt(4, companion.level()); statement.setLong(5, companion.experience()); statement.setBoolean(6, companion.unlocked()); statement.setBoolean(7, companion.active());
                    statement.setString(8, companion.skinValue()); statement.setString(9, companion.skinSignature());
                    statement.setString(10, itemYaml(companion.equipment().get("helmet"))); statement.setString(11, itemYaml(companion.equipment().get("chestplate")));
                    statement.setString(12, itemYaml(companion.equipment().get("leggings"))); statement.setString(13, itemYaml(companion.equipment().get("boots")));
                    statement.setString(14, itemYaml(companion.equipment().get("main-hand"))); statement.setString(15, itemYaml(companion.equipment().get("off-hand"))); statement.addBatch();
                } statement.executeBatch();
            }
            try (PreparedStatement statement = connection.prepareStatement(deleteStatsSql)) { statement.setString(1, profile.getUuid().toString()); statement.executeUpdate(); } try (PreparedStatement statement = connection.prepareStatement(upsertStatSql)) { for (var entry : statistics.entrySet()) { statement.setString(1, profile.getUuid().toString()); statement.setString(2, entry.getKey()); statement.setLong(3, entry.getValue()); statement.addBatch(); } for (Profession profession : Profession.values()) { statement.setString(1, profile.getUuid().toString()); statement.setString(2, PROFESSION_LEVEL_PREFIX + profession.name().toLowerCase()); statement.setLong(3, profile.getProfessionLevel(profession)); statement.addBatch(); statement.setString(2, PROFESSION_XP_PREFIX + profession.name().toLowerCase()); statement.setLong(3, profile.getProfessionExperience(profession)); statement.addBatch(); statement.setString(2, PROFESSION_LEARNED_PREFIX + profession.name().toLowerCase()); statement.setLong(3, profile.hasLearnedProfession(profession) ? 1L : 0L); statement.addBatch(); } for (String recipeId : unlockedRecipes) { statement.setString(1, profile.getUuid().toString()); statement.setString(2, RECIPE_UNLOCK_PREFIX + recipeId); statement.setLong(3, 1L); statement.addBatch(); } statement.executeBatch(); }
            connection.commit(); } catch (SQLException e) { connection.rollback(); throw e; } finally { connection.setAutoCommit(true); } }
        return nextRevision;
    }
    private static final class MapBuilder { private final Map<EquipmentSlot, ItemStack> values = new EnumMap<>(EquipmentSlot.class); void put(EquipmentSlot slot, ItemStack item) { values.put(slot, item); } Map<EquipmentSlot, ItemStack> values() { return values; } }
    @Override public void shutdown() { databaseManager.shutdown(); }
}
