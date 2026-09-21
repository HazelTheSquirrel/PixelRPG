package de.pixelrpg.rpg.npc;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

public final class NpcPresentationService {
    private final Plugin plugin;
    private final NpcManager npcManager;
    private final NpcProfileStore profileStore;

    public NpcPresentationService(Plugin plugin, NpcManager npcManager, NpcProfileStore profileStore) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.npcManager = Objects.requireNonNull(npcManager, "npcManager");
        this.profileStore = Objects.requireNonNull(profileStore, "profileStore");
    }

    public void refreshAll() {
        for (RPGNpc npc : npcManager.getAll()) {
            refresh(npc);
        }
    }

    public void refresh(RPGNpc npc) {
        java.util.UUID entityUuid = npcManager.getSpawnedEntityUuid(npc.id()).orElse(null);
        if (entityUuid == null) return;
        Entity entity = Bukkit.getEntity(entityUuid);
        if (entity instanceof Mannequin mannequin && entity.isValid()) {
            NpcProfile profile = profileStore.get(npc.id()).orElse(null);
            if (profile == null) return;
            String title = profile.title();
            Component display = title.isBlank()
                    ? Component.text(npc.name(), npc.type().getColor())
                    : Component.text(npc.name() + " • " + title, npc.type().getColor());
            mannequin.customName(display);
            if (npc.hasCustomSkin()) {
                MannequinSkinResolver.apply(mannequin, npc.skinSource(), plugin.getLogger());
            }
        }
    }

}
