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
    private volatile ReflectionBridge reflectionBridge;
    private volatile PacketBridge packetBridge;

    public NpcNameVisibilityService(Logger logger) {
        this.logger = logger;
    }

    public void setVisible(Player player, org.bukkit.entity.Entity entity, boolean visible) {
        if (player == null || entity == null || !player.isOnline() || !entity.isValid()) return;

        try {
            ReflectionBridge bridge = getReflectionBridge();
            Object dataValue = bridge.dataValueFactory().invoke(null, bridge.dataAccessor(), visible);
            Object packet = bridge.packetConstructor().newInstance(entity.getEntityId(), List.of(dataValue));
            Object connection = player.getConnection();
            PacketBridge packetBridge = getPacketBridge(connection);
            Object packetListener = packetBridge.packetListenerField().get(connection);
            packetBridge.sendMethod().invoke(packetListener, packet);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            logger.log(Level.WARNING,
                    "Failed to update NPC name visibility for " + player.getName(),
                    exception);
        }
    }

    private ReflectionBridge getReflectionBridge() throws ReflectiveOperationException {
        ReflectionBridge current = reflectionBridge;
        if (current != null) return current;

        synchronized (this) {
            current = reflectionBridge;
            if (current == null) {
                current = ReflectionBridge.create();
                reflectionBridge = current;
            }
            return current;
        }
    }

    private PacketBridge getPacketBridge(Object connection) throws ReflectiveOperationException {
        PacketBridge current = packetBridge;
        if (current != null && current.connectionType().isInstance(connection)) return current;

        synchronized (this) {
            current = packetBridge;
            if (current == null || !current.connectionType().isInstance(connection)) {
                Object packetListener = findPacketListener(connection);
                current = PacketBridge.create(connection.getClass(), packetListener.getClass());
                packetBridge = current;
            }
            return current;
        }
    }

    private static Object findPacketListener(Object connection) throws ReflectiveOperationException {
        Field field = findPacketListenerField(connection.getClass());
        return field.get(connection);
    }

    private static Field findPacketListenerField(Class<?> type) throws ReflectiveOperationException {
        Class<?> current = type;
        while (current != null) {
            try {
                Field field = current.getDeclaredField("packetListener");
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
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

    private record ReflectionBridge(
            Object dataAccessor,
            Method dataValueFactory,
            Constructor<?> packetConstructor
    ) {
        private static ReflectionBridge create() throws ReflectiveOperationException {
            Class<?> entityClass = Class.forName(ENTITY_CLASS);
            Field accessorField = entityClass.getDeclaredField("DATA_CUSTOM_NAME_VISIBLE");
            accessorField.setAccessible(true);
            Object accessor = accessorField.get(null);

            Class<?> dataAccessorClass = Class.forName(DATA_ACCESSOR_CLASS);
            Class<?> dataValueClass = Class.forName(DATA_VALUE_CLASS);
            Method factory = dataValueClass.getMethod("create", dataAccessorClass, Object.class);
            factory.setAccessible(true);

            Class<?> packetClass = Class.forName(PACKET_CLASS);
            Constructor<?> packetConstructor = packetClass.getConstructor(int.class, List.class);
            packetConstructor.setAccessible(true);

            return new ReflectionBridge(accessor, factory, packetConstructor);
        }
    }

    private record PacketBridge(
            Class<?> connectionType,
            Field packetListenerField,
            Method sendMethod
    ) {
        private static PacketBridge create(Class<?> connectionType, Class<?> packetListenerType) throws ReflectiveOperationException {
            return new PacketBridge(
                    connectionType,
                    findPacketListenerField(connectionType),
                    findSendMethod(packetListenerType)
            );
        }
    }
}
