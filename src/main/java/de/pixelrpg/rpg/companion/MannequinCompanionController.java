package de.pixelrpg.rpg.companion;

import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.npc.MannequinSkinResolver;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Controls only Unique Mannequin Companion presentation, including its configured NPC skin source. */
public final class MannequinCompanionController {
    private final Plugin plugin;
    private final CompanionRegistry registry;
    private final CompanionService companionService;
    private final Map<UUID, String> appliedSkins = new HashMap<>();

    public MannequinCompanionController(Plugin plugin, CompanionService companionService, CompanionRegistry registry) {
        this.plugin = plugin;
        this.companionService = companionService;
        this.registry = registry;
    }

    public void tick(Player owner, Mannequin mannequin) {
        if (!mannequin.isValid() || !owner.isOnline()) return;
        String id = mannequin.getPersistentDataContainer().get(RPGKeys.Companion.id(), PersistentDataType.STRING);
        if (id == null || id.isBlank()) return;

        CompanionDefinition definition = registry.find(id).orElse(null);
        if (definition == null || !definition.rarity().isUnique()
                || definition.visual().type() != CompanionDefinition.CompanionVisualDefinition.VisualType.MANNEQUIN) {
            return;
        }

        var scale = mannequin.getAttribute(Attribute.SCALE);
        if (scale != null && definition.visual().scale() > 0.0D) {
            scale.setBaseValue(definition.visual().scale());
        }

        String skin = definition.visual().skinSource();
        UUID mannequinId = mannequin.getUniqueId();
        CompanionService.StoredSkin stored = companionService.getStoredSkin(owner.getUniqueId(), id);
        if (stored != null) {
            String storedKey = stored.value() + "|" + String.valueOf(stored.signature());
            if (!storedKey.equals(appliedSkins.get(mannequinId))) {
                MannequinSkinResolver.applyStoredTexture(mannequin, stored.value(), stored.signature(), plugin)
                        .thenRun(() -> appliedSkins.put(mannequinId, storedKey));
            }
        } else if (!skin.isBlank() && !skin.equals(appliedSkins.get(mannequinId))) {
            MannequinSkinResolver.apply(mannequin, skin, plugin.getLogger())
                    .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                        if (!mannequin.isValid()) return;
                        mannequin.getProfile().properties().stream()
                                .filter(property -> "textures".equals(property.getName()))
                                .findFirst()
                                .ifPresent(property -> {
                                    companionService.storeSkin(owner.getUniqueId(), id, property.getValue(), property.getSignature());
                                    appliedSkins.put(mannequinId, property.getValue() + "|" + String.valueOf(property.getSignature()));
                                });
                    }));
        }

        mannequin.setImmovable(false);
    }
}
