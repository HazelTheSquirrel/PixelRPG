package de.pixelrpg.rpg.npc;

import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Sends the native entity name visibility metadata to individual Paper clients. */
public final class NpcNameVisibilityService {
    private static final String ENTITY_CLASS = "net.minecraft.world.entity.Entity";
    private static final String DATA_ACCESSOR_CLASS = "net.minecraft.network.syncher.EntityDataAccessor";
    private static final String DATA_VALUE_CLASS = "net.minecraft.network.syncher.SynchedEntityData$DataValue";
    private static final String PACKET_CLASS = "net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket";

    private final Logger logger;

    public NpcNameVisibilityService(Logger logger) {
        this.logger = logger;
    }

    public void setVisible(Player player, org.bukkit.entity.Entity entity, boolean visible) {
        if (!player.isOnline() || !entity.isValid()) return;

        try {
            Object dataAccessor = resolveCustomNameVisibilityAccessor();
            Object dataValue = createDataValue(dataAccessor, visible);
            Object packet = createEntityDataPacket(entity.getEntityId(), dataValue);
            sendPacket(player, packet);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            logger.log(Level.WARNING,
                    "Failed to update NPC name visibility for " + player.getName(),
                    exception);
        }
    }

    private static Object resolveCustomNameVisibilityAccessor() throws ReflectiveOperationException {
        Class<?> entityClass = Class.forName(ENTITY_CLASS);
        Field field = entityClass.getDeclaredField("DATA_CUSTOM_NAME_VISIBLE");
        field.setAccessible(true);
        return field.get(null);
    }

    private static Object createDataValue(Object dataAccessor, boolean visible) throws ReflectiveOperationException {
        Class<?> dataValueClass = Class.forName(DATA_VALUE_CLASS);
        Method create = dataValueClass.getMethod("create", Class.forName(DATA_ACCESSOR_CLASS), Object.class);
        return create.invoke(null, dataAccessor, visible);
    }

    private static Object createEntityDataPacket(int entityId, Object dataValue) throws ReflectiveOperationException {
        Class<?> packetClass = Class.forName(PACKET_CLASS);
        Constructor<?> constructor = packetClass.getConstructor(int.class, List.class);
        return constructor.newInstance(entityId, List.of(dataValue));
    }

    private static void sendPacket(Player player, Object packet) throws ReflectiveOperationException {
        Object connection = player.getConnection();
        Object packetListener = findPacketListener(connection);
        Method send = findSendMethod(packetListener.getClass());
        send.invoke(packetListener, packet);
    }

    private static Object findPacketListener(Object connection) throws ReflectiveOperationException {
        Class<?> type = connection.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField("packetListener");
                field.setAccessible(true);
                return field.get(connection);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException("packetListener");
    }

    private static Method findSendMethod(Class<?> type) throws ReflectiveOperationException {
        Class<?> current = type;
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (!method.getName().equals("send") || method.getParameterCount() != 1) continue;
                method.setAccessible(true);
                return method;
            }
            current = current.getSuperclass();
        }
        throw new NoSuchMethodException("send(Packet)");
    }
}
