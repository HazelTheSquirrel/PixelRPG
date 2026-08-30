package de.pixelrpg.rpg.region;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Owns temporary admin-only polygon creation sessions and their live visualization. */
public final class RegionEditor {
    private final JavaPlugin plugin;
    private final RegionManager regions;
    private final NamespacedKey toolKey;
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();
    private BukkitTask visualizationTask;

    public RegionEditor(JavaPlugin plugin, RegionManager regions) {
        this.plugin = plugin;
        this.regions = regions;
        this.toolKey = new NamespacedKey(plugin, "region_editor_tool");
    }

    public void start() {
        visualizationTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::visualize, 1L, 2L);
    }

    public void shutdown() {
        if (visualizationTask != null) visualizationTask.cancel();
        sessions.clear();
    }

    public void begin(Player player, String name) {
        sessions.put(player.getUniqueId(), new Session(UUID.randomUUID(), name));
        removeTools(player);
        player.getInventory().addItem(createTool());
        player.sendMessage(Component.text("Region-CREATE gestartet: " + name, NamedTextColor.GREEN));
        player.sendMessage(Component.text("Rechtsklick auf einen Block setzt P1, P2, P3 ...", NamedTextColor.GRAY));
        player.sendMessage(Component.text("/pixelrpg region finish → prüfen | confirm → speichern | cancel → abbrechen", NamedTextColor.GRAY));
    }

    public boolean isEditing(UUID playerId) {
        return sessions.containsKey(playerId);
    }

    public boolean addPoint(Player player, Location clicked) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null || clicked.getWorld() == null) return false;
        if (session.worldName == null) session.worldName = clicked.getWorld().getName();
        if (!session.worldName.equals(clicked.getWorld().getName())) {
            player.sendMessage(Component.text("Alle Punkte müssen in derselben Welt liegen.", NamedTextColor.RED));
            return true;
        }

        RegionPoint point = new RegionPoint(clicked.getBlockX() + 0.5D, clicked.getBlockZ() + 0.5D);
        if (session.points.contains(point)) {
            player.sendMessage(Component.text("Dieser Punkt wurde bereits gesetzt.", NamedTextColor.RED));
            return true;
        }

        session.points.add(point);
        session.finished = false;
        player.sendMessage(Component.text("P" + session.points.size() + " gesetzt: " + point.x() + ", " + point.z(), NamedTextColor.AQUA));
        return true;
    }

    public void finish(Player player) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;
        RegionGeometry.ValidationResult validation = RegionGeometry.validate(session.points);
        if (!validation.valid()) {
            player.sendMessage(Component.text("Polygon ungültig: " + validation.error(), NamedTextColor.RED));
            return;
        }
        session.finished = true;
        player.sendMessage(Component.text("Polygon gültig. " + session.points.size() + " Punkte, Fläche "
                + String.format(java.util.Locale.ROOT, "%.2f", validation.geometry().area())
                + " Blöcke².", NamedTextColor.GREEN));
        player.sendMessage(Component.text("/pixelrpg region confirm zum Erstellen.", NamedTextColor.GRAY));
    }

    public void confirm(Player player) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;
        if (!session.finished) {
            player.sendMessage(Component.text("Bitte zuerst /pixelrpg region finish ausführen.", NamedTextColor.YELLOW));
            return;
        }
        if (session.worldName == null) {
            player.sendMessage(Component.text("Es wurde noch kein Punkt gesetzt.", NamedTextColor.RED));
            return;
        }

        World world = plugin.getServer().getWorld(session.worldName);
        if (world == null) {
            player.sendMessage(Component.text("Die Welt der Region ist nicht geladen.", NamedTextColor.RED));
            return;
        }

        RegionGeometry.ValidationResult result = regions.create(
                session.id,
                session.worldName,
                session.points,
                world.getMinHeight(),
                world.getMaxHeight() - 1,
                session.name,
                RegionType.OTHER
        );
        if (!result.valid()) {
            player.sendMessage(Component.text("Region konnte nicht erstellt werden: " + result.error(), NamedTextColor.RED));
            return;
        }

        sessions.remove(player.getUniqueId());
        removeTools(player);
        player.sendMessage(Component.text("Region „" + session.name + "“ erstellt. ID: " + session.id, NamedTextColor.GREEN));
    }

    public void cancel(Player player) {
        if (sessions.remove(player.getUniqueId()) != null) {
            removeTools(player);
            player.sendMessage(Component.text("Region-CREATE abgebrochen. Es wurde keine Region verändert.", NamedTextColor.YELLOW));
        }
    }

    public boolean isTool(ItemStack item) {
        if (item == null || item.getType() != Material.STICK) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(toolKey, PersistentDataType.BYTE);
    }

    private ItemStack createTool() {
        ItemStack item = new ItemStack(Material.STICK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("PixelRPG Region-Werkzeug", NamedTextColor.GOLD));
        meta.getPersistentDataContainer().set(toolKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private void removeTools(Player player) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (isTool(item)) player.getInventory().setItem(slot, null);
        }
    }

    private void visualize() {
        for (Map.Entry<UUID, Session> entry : sessions.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) continue;
            Session session = entry.getValue();
            World world = session.worldName == null ? player.getWorld() : plugin.getServer().getWorld(session.worldName);
            if (world == null || session.points.isEmpty()) continue;

            double y = player.getLocation().getY() + 1.0D;
            for (int i = 0; i < session.points.size(); i++) {
                RegionPoint point = session.points.get(i);
                Location marker = new Location(world, point.x(), y, point.z());
                for (int j = 0; j < 6; j++) {
                    player.spawnParticle(Particle.END_ROD, marker.clone().add(0, j, 0), 2, 0.05, 0.05, 0.05, 0.0);
                }
                if (i > 0) drawLine(player, world, session.points.get(i - 1), point, y);
            }
            if (session.points.size() >= 2) drawLine(player, world, session.points.getLast(), session.points.getFirst(), y);
        }
    }

    private void drawLine(Player player, World world, RegionPoint a, RegionPoint b, double y) {
        double dx = b.x() - a.x();
        double dz = b.z() - a.z();
        double length = Math.sqrt(dx * dx + dz * dz);
        int steps = Math.max(1, (int) Math.ceil(length * 1.5D));
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            player.spawnParticle(Particle.END_ROD, new Location(world, a.x() + dx * t, y, a.z() + dz * t), 1, 0, 0, 0, 0);
        }
    }

    private static final class Session {
        private final UUID id;
        private final String name;
        private final ArrayList<RegionPoint> points = new ArrayList<>();
        private String worldName;
        private boolean finished;

        private Session(UUID id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
