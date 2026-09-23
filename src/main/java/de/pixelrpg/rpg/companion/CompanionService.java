package de.pixelrpg.rpg.companion;

import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.npc.MannequinSkinResolver;
import de.pixelrpg.rpg.player.CompanionState;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Owns companion domain mutations while PlayerProfile remains the single source of persistent truth. */
public final class CompanionService {
    private final Plugin plugin;
    private final PlayerProfileManager profiles;
    private final CompanionRegistry registry;
    private final CompanionRuntimeListener runtime;
    private final Map<UUID, UUID> activeEntities = new ConcurrentHashMap<>();

    public CompanionService(Plugin plugin, PlayerProfileManager profiles, CompanionRegistry registry) {
        this.plugin = Objects.requireNonNull(plugin);
        this.profiles = Objects.requireNonNull(profiles);
        this.registry = Objects.requireNonNull(registry);
        this.runtime = new CompanionRuntimeListener(plugin, this);
    }

    public Listener runtimeListener() { return runtime; }
    public CompanionRegistry registry() { return registry; }
    public java.util.List<String> definitionIds() { return registry.definitions().keySet().stream().sorted().toList(); }
    public CompanionDefinition definition(String id) { return registry.require(id); }

    public java.util.List<Companion> getCompanions(UUID playerId) {
        PlayerProfile profile = profiles.getProfile(playerId).orElse(null);
        if (profile == null) return java.util.List.of();
        return profile.getCompanions().values().stream()
                .filter(CompanionState::unlocked)
                .map(state -> view(state, registry.find(state.id()).orElse(null)))
                .filter(Objects::nonNull).toList();
    }

    public Companion getActive(UUID playerId) {
        PlayerProfile profile = profiles.getProfile(playerId).orElse(null);
        if (profile == null) return null;
        return profile.getCompanions().values().stream().filter(CompanionState::active).findFirst()
                .map(state -> view(state, registry.find(state.id()).orElse(null))).orElse(null);
    }

    public UUID getActiveEntity(UUID playerId) { return activeEntities.get(playerId); }
    public UUID getOwnerOfEntity(UUID entityId) {
        for (var entry : activeEntities.entrySet()) if (entry.getValue().equals(entityId)) return entry.getKey();
        return null;
    }

    public boolean adminGrant(UUID playerId, String id) {
        CompanionDefinition definition = registry.find(id).orElse(null);
        return definition != null && grant(playerId, definition);
    }

    public boolean unlockDefinition(UUID playerId, String id) {
        CompanionDefinition definition = registry.find(id).orElse(null);
        if (definition == null || definition.adminOnly()) return false;
        return grant(playerId, definition);
    }

    public boolean unlockFromBoss(UUID playerId, String bossId) {
        if (bossId == null || bossId.isBlank()) return false;
        return registry.definitions().values().stream()
                .filter(def -> "BOSS".equalsIgnoreCase(def.unlock().type()))
                .filter(def -> bossId.equalsIgnoreCase(def.unlock().bossId()))
                .findFirst().map(def -> grant(playerId, def)).orElse(false);
    }

    public boolean setActive(Player player, String id) {
        PlayerProfile profile = profiles.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || profile.getCompanion(id) == null) return false;
        for (CompanionState state : profile.getCompanions().values()) {
            if (state.active()) profile.setCompanion(state.withActive(false));
        }
        CompanionState selected = profile.getCompanion(id);
        profile.setCompanion(selected.withActive(true));
        profiles.saveProfileAsync(player.getUniqueId());
        despawn(player.getUniqueId());
        spawn(player, selected.withActive(true));
        return true;
    }

    public void clearActive(Player player) { clearActive(player.getUniqueId()); }

    public void clearActive(UUID playerId) {
        PlayerProfile profile = profiles.getProfile(playerId).orElse(null);
        if (profile == null) return;
        for (CompanionState state : profile.getCompanions().values()) if (state.active()) profile.setCompanion(state.withActive(false));
        profiles.saveProfileAsync(playerId);
        despawn(playerId);
    }

    public void restoreActive(Player player) {
        PlayerProfile profile = profiles.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || activeEntities.containsKey(player.getUniqueId())) return;
        CompanionState active = profile.getCompanions().values().stream().filter(CompanionState::active).findFirst().orElse(null);
        if (active != null) spawn(player, active);
    }

    public void despawn(UUID playerId) {
        UUID entityId = activeEntities.remove(playerId);
        if (entityId == null) return;
        Entity entity = plugin.getServer().getEntity(entityId);
        if (entity != null) entity.remove();
    }

    public void rename(UUID playerId, String id, String name) {
        PlayerProfile profile = profiles.getProfile(playerId).orElse(null);
        CompanionState state = profile == null ? null : profile.getCompanion(id);
        CompanionDefinition definition = registry.find(id).orElse(null);
        String cleaned = name == null ? "" : name.strip();
        if (profile == null || state == null || definition == null || !definition.renameable()
                || cleaned.isBlank() || cleaned.length() > registry.maxNameLength()) return;
        profile.setCompanion(state.withName(cleaned));
        profiles.saveProfileAsync(playerId);
        UUID entityId = activeEntities.get(playerId);
        if (entityId != null) {
            Entity entity = plugin.getServer().getEntity(entityId);
            if (entity instanceof LivingEntity living) living.customName(Component.text(cleaned));
        }
    }

    public boolean awardExperience(UUID playerId, long baseExperience) {
        PlayerProfile profile = profiles.getProfile(playerId).orElse(null);
        if (profile == null || baseExperience <= 0L) return false;
        CompanionState active = profile.getCompanions().values().stream().filter(CompanionState::active).findFirst().orElse(null);
        if (active == null) return false;
        CompanionDefinition definition = registry.find(active.id()).orElse(null);
        if (definition == null || definition.rarity().isUnique()) return false;
        int maxLevel = Math.min(registry.maxLevel(), definition.progression().maxLevel());
        long total = active.experience() + Math.max(1L, Math.round(baseExperience * definition.rarity().experienceMultiplier()));
        int level = levelForExperience(definition, total, maxLevel);
        if (total == active.experience()) return false;
        profile.setCompanion(active.withProgress(level, total));
        profiles.saveProfileAsync(playerId);
        runtime.wake(playerId);
        return level != active.level();
    }

    public Map<String, ItemStack> equipment(UUID playerId, String companionId) {
        PlayerProfile profile = profiles.getProfile(playerId).orElse(null);
        CompanionState state = profile == null ? null : profile.getCompanion(companionId);
        if (state == null) return Map.of();
        return state.equipmentCopy();
    }

    public void setEquipment(UUID playerId, String companionId, Map<String, ItemStack> equipment) {
        PlayerProfile profile = profiles.getProfile(playerId).orElse(null);
        CompanionState state = profile == null ? null : profile.getCompanion(companionId);
        if (state == null) return;
        profile.setCompanion(state.withEquipment(equipment));
        profiles.saveProfileAsync(playerId);
        runtime.wake(playerId);
    }

    public void storeSkin(UUID playerId, String companionId, String value, String signature) {
        if (value == null || value.isBlank()) return;
        PlayerProfile profile = profiles.getProfile(playerId).orElse(null);
        CompanionState state = profile == null ? null : profile.getCompanion(companionId);
        if (state == null) return;
        profile.setCompanion(state.withSkin(value, signature));
        profiles.saveProfileAsync(playerId);
    }

    public String companionId(Entity entity) { return entity == null ? null : entity.getPersistentDataContainer().get(new RPGKeys(plugin).companionId(), PersistentDataType.STRING); }
    public boolean isCompanionEntity(Entity entity) { String id = companionId(entity); return id != null && registry.find(id).isPresent(); }

    public CompanionState state(UUID playerId, String companionId) {
        PlayerProfile profile = profiles.getProfile(playerId).orElse(null);
        return profile == null ? null : profile.getCompanion(companionId);
    }

    public void shutdown() {
        runtime.shutdown();
        for (UUID owner : activeEntities.keySet()) despawn(owner);
        activeEntities.clear();
    }

    void bindEntity(UUID playerId, UUID entityId) { activeEntities.put(playerId, entityId); }
    void unbindEntity(UUID playerId, UUID entityId) { activeEntities.remove(playerId, entityId); }

    private boolean grant(UUID playerId, CompanionDefinition definition) {
        PlayerProfile profile = profiles.getProfile(playerId).orElse(null);
        if (profile == null || profile.getCompanion(definition.id()) != null) return false;
        Map<String, ItemStack> equipment = new LinkedHashMap<>();
        defaults(definition.equipment(), equipment);
        profile.setCompanion(new CompanionState(definition.id(), definition.displayName(), 1, 0L, true, false, equipment, "", ""));
        profiles.saveProfileAsync(playerId);
        return true;
    }

    private void spawn(Player player, CompanionState state) {
        CompanionDefinition definition = registry.find(state.id()).orElse(null);
        if (definition == null) return;
        try {
            Entity entity = player.getWorld().spawnEntity(player.getLocation().clone().add(1, 0, 1), definition.entityType());
            if (!(entity instanceof LivingEntity living)) { entity.remove(); return; }
            living.getPersistentDataContainer().set(new RPGKeys(plugin).companionId(), PersistentDataType.STRING, definition.id());
            living.getPersistentDataContainer().set(new RPGKeys(plugin).companionLevel(), PersistentDataType.INTEGER, state.level());
            living.getPersistentDataContainer().set(new RPGKeys(plugin).companionRarity(), PersistentDataType.STRING, definition.rarity().name());
            living.customName(Component.text(state.name())); living.setCustomNameVisible(true);
            living.setInvulnerable(!definition.rarity().isUnique());
            if (living instanceof Mob mob) { mob.setAware(true); mob.setTarget(null); }
            applyEquipment(state, living);
            activeEntities.put(player.getUniqueId(), living.getUniqueId());
            if (living instanceof Mannequin mannequin) applySkin(player, state, definition, mannequin);
            runtime.wake(player.getUniqueId());
        } catch (RuntimeException exception) {
            plugin.getLogger().log(java.util.logging.Level.WARNING, "Unable to spawn companion " + state.id(), exception);
        }
    }

    private void applySkin(Player player, CompanionState state, CompanionDefinition definition, Mannequin mannequin) {
        if (!state.skinValue().isBlank()) {
            MannequinSkinResolver.applyStoredTexture(mannequin, state.skinValue(), state.skinSignature(), plugin);
            return;
        }
        String source = definition.visual().get("skinSource") != null ? definition.visual().get("skinSource").getAsString() : "";
        if (source.isBlank()) return;
        MannequinSkinResolver.applyAndCapture(mannequin, source, plugin.getLogger()).thenAccept(texture ->
                plugin.getServer().getScheduler().runTask(plugin, () -> storeSkin(player.getUniqueId(), state.id(), texture.getValue(), texture.getSignature())));
    }

    private static void applyEquipment(CompanionState state, LivingEntity entity) {
        var equipment = entity.getEquipment(); if (equipment == null) return;
        equipment.setHelmet(state.equipment().get("helmet"), true); equipment.setChestplate(state.equipment().get("chestplate"), true);
        equipment.setLeggings(state.equipment().get("leggings"), true); equipment.setBoots(state.equipment().get("boots"), true);
        equipment.setItemInMainHand(state.equipment().get("main-hand"), true); equipment.setItemInOffHand(state.equipment().get("off-hand"), true);
    }

    private static void defaults(CompanionDefinition.EquipmentDefaults defaults, Map<String, ItemStack> result) {
        put(result,"helmet",defaults.helmet()); put(result,"chestplate",defaults.chestplate()); put(result,"leggings",defaults.leggings());
        put(result,"boots",defaults.boots()); put(result,"main-hand",defaults.mainHand()); put(result,"off-hand",defaults.offHand());
    }
    private static void put(Map<String,ItemStack> result,String slot,String material){if(material==null||material.isBlank())return;Material type=Material.matchMaterial(material);if(type!=null&&!type.isAir())result.put(slot,new ItemStack(type));}

    private static Companion view(CompanionState state, CompanionDefinition definition) {
        return definition == null ? null : new Companion(state.id(), state.name(), state.level(), state.experience(), definition.rarity(), definition.entityType(), state.active());
    }

    private static int levelForExperience(CompanionDefinition definition,long experience,int maxLevel) {
        long remaining=Math.max(0L,experience); int level=1;
        while(level<maxLevel){long required=registryXp(definition,level);if(remaining<required)break;remaining-=required;level++;}
        return level;
    }
    private static long registryXp(CompanionDefinition definition,int level){return 100L + (long)level*8L;}

    public record StoredSkin(String value,String signature) {}
}
