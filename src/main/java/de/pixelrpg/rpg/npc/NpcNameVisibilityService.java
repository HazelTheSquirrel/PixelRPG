package de.pixelrpg.rpg.npc;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Sends the native entity name visibility metadata to individual Paper clients. */
public final class NpcNameVisibilityService {
    private static final EntityDataAccessor<Boolean> DATA_CUSTOM_NAME_VISIBLE = resolveCustomNameVisibilityAccessor();

    private final Logger logger;

    public NpcNameVisibilityService(Logger logger) {
        this.logger = logger;
    }

    public void setVisible(Player player, org.bukkit.entity.Entity entity, boolean visible) {
        if (!player.isOnline() || !entity.isValid()) return;

        SynchedEntityData.DataValue<Boolean> value = SynchedEntityData.DataValue.create(
                DATA_CUSTOM_NAME_VISIBLE,
                visible
        );
        Packet<?> packet = new ClientboundSetEntityDataPacket(entity.getEntityId(), List.of(value));

        try {
            Object connection = player.getConnection();
            Method send = connection.getClass().getMethod("send", Packet.class);
            send.invoke(connection, packet);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            logger.log(Level.WARNING, "Failed to update NPC name visibility for " + player.getName(), exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static EntityDataAccessor<Boolean> resolveCustomNameVisibilityAccessor() {
        try {
            Field field = Entity.class.getDeclaredField("DATA_CUSTOM_NAME_VISIBLE");
            field.setAccessible(true);
            return (EntityDataAccessor<Boolean>) field.get(null);
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
