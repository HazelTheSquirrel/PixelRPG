package de.pixelrpg.rpg.companion;

import de.pixelrpg.rpg.core.RPGKeys;
import de.pixelrpg.rpg.npc.MannequinSkinResolver;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Controls only Mannequin-specific presentation; follow, stats and combat are shared runtime systems. */
public final class MannequinCompanionController {
    private final Plugin plugin;
    private final CompanionRegistry registry;
    private final Map<UUID, String> appliedSkins = new HashMap<>();

    public MannequinCompanionController(Plugin plugin, CompanionService ignoredCompanionService, CompanionRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    public void tick(Player owner, Mannequin mannequin) {
        if (!mannequin.isValid() || !owner.isOnline()) return;
        String id = mannequin.getPersistentDataContainer().get(RPGKeys.Companion.id(), PersistentDataType.STRING);
        if (id == null || id.isBlank()) return;
        CompanionDefinition definition = registry.find(id).orElse(null);
        if (definition == null) return;

        var scale = mannequin.getAttribute(Attribute.SCALE);
        if (scale != null && definition.visual().scale() > 0.0D) scale.setBaseValue(definition.visual().scale());

        String skin = definition.visual().skinSource();
        if (!skin.isBlank() && !skin.equals(appliedSkins.get(mannequin.getUniqueId()))) {
            appliedSkins.put(mannequin.getUniqueId(), skin);
            MannequinSkinResolver.apply(mannequin, skin, plugin.getLogger());
        }
        mannequin.setImmovable(false);
    }
}
