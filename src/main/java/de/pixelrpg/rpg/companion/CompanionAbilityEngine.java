package de.pixelrpg.rpg.companion;

import com.google.gson.JsonObject;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Data-driven ability runtime with generic cooldown handling; concrete effects are intentionally definition-driven. */
public final class CompanionAbilityEngine {
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    public boolean isOnCooldown(UUID ownerUuid, String abilityId, long gameTime) {
        return cooldowns.getOrDefault(ownerUuid, Map.of()).getOrDefault(abilityId, 0L) > gameTime;
    }

    public void triggerCooldown(UUID ownerUuid, String abilityId, long gameTime, long cooldownTicks) {
        cooldowns.computeIfAbsent(ownerUuid, ignored -> new ConcurrentHashMap<>())
                .put(abilityId, gameTime + Math.max(0L, cooldownTicks));
    }

    public boolean execute(String abilityId, JsonObject definition, Player owner, LivingEntity companion, LivingEntity target, long gameTime) {
        if (definition == null || isOnCooldown(owner.getUniqueId(), abilityId, gameTime)) return false;
        String type = definition.has("type") ? definition.get("type").getAsString().toUpperCase() : "PASSIVE";
        if ("ACTIVE".equals(type)) {
            long cooldown = definition.has("cooldownTicks") ? definition.get("cooldownTicks").getAsLong() : 0L;
            triggerCooldown(owner.getUniqueId(), abilityId, gameTime, cooldown);
        }
        return true;
    }

    public void clear(UUID ownerUuid) { cooldowns.remove(ownerUuid); }
}
