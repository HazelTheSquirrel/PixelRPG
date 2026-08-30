package de.pixelrpg.rpg.region;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.NamespacedKey;

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

    public void begin(Player player, String name, RegionType type, int minY, int maxY) {
        sessions.put(player.getUniqueId(), new Session(UUID.randomUUID(), name, type, minY, maxY));
        removeTools(player);
        player.getInventory().addItem(createTool());
        player.sendMessage(Component.text("Region-CREATE gestartet. Rechtsklick auf Blöcke setzt Konturpunkte.", NamedTextColor.GREEN));
        player.sendMessage(Component.text("/pixelrpgadmin region finish → validieren | confirm → speichern | cancel → abbrechen", NamedTextColor.GRAY));
    }

    public boolean isEditing(UUID playerId) { return sessions.containsKey(playerId); }

    public boolean addPoint(Player player, Location clicked) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null || clicked.getWorld() == null) return false;
        if (session.worldName == null) session.worldName = clicked.getWorld().getName();
        if (!session.worldName.equals(clicked.getWorld().getName())) {
            player.sendMessage(Component.text("Alle Punkte einer Region müssen in derselben Welt liegen.", NamedTextColor.RED));
            return true;
        }
        RegionPoint point = new RegionPoint(clicked.getBlockX() + 0.5D, clicked.getBlockZ() + 0.5D);
        if (!session.points.isEmpty()) {
            RegionPoint last = session.points.getLast();
            if (last.equals(point)) {
                player.sendMessage(Component.text("Dieser Punkt ist bereits der letzte gesetzte Punkt.", NamedTextColor.RED));
                return true;
            }
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
        player.sendMessage(Component.text("Polygon gültig. Fläche: " + String.format(java.util.Locale.ROOT, "%.2f", validation.geometry().area()) + " Blöcke². Nutze /pixelrpgadmin region confirm zum Erstellen.", NamedTextColor.GREEN));
    }

    public void confirm(Player player) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;
        if (!session.finished) {
            player.sendMessage(Component.text("Bitte zuerst /pixelrpgadmin region finish ausführen.", NamedTextColor.YELLOW));
            return;
        }
        if (session.worldName == null) {
            player.sendMessage(Component.text("Es wurde noch kein Punkt gesetzt.", NamedTextColor.RED));
            return;
        }
        RegionGeometry.ValidationResult result = regions.create(session.id, session.worldName, session.points, session.minY, session.maxY, session.name, session.type);
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
        if (item == null || item.getType().isAir()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(toolKey, PersistentDataType.BYTE);
    }

    private ItemStack createTool() {
        ItemStack item = new ItemStack(org.bukkit.Material.BLAZE_ROD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("PixelRPG Region-Werkzeug", NamedTextColor.GOLD));
        meta.getPersistentDataContainer().set(toolKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private void removeTools(Player player) {
        player.getInventory().removeIf(this::isTool);
    }

    private void visualize() {
        for (Map.Entry<UUID, Session> entry : sessions.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) continue;
            Session session = entry.getValue();
            World world = session.worldName == null ? player.getWorld() : plugin.getServer().getWorld(session.worldName);
            if (world == null || session.points.isEmpty()) continue;
            double y = Math.max(player.getLocation().getY(), session.minY) + 1.0D;
            for (int i = 0; i < session.points.size(); i++) {
                RegionPoint point = session.points.get(i);
                Location marker = new Location(world, point.x(), y, point.z());
                for (int j = 0; j < 6; j++) player.spawnParticle(Particle.END_ROD, marker.clone().add(0, j * 1.0D, 0), 2, 0.05, 0.05, 0.05, 0.0);
                if (i > 0) drawLine(player, world, session.points.get(i - 1), point, y);
            }
            if (session.points.size() >= 2) drawLine(player, world, session.points.getLast(), session.points.getFirst(), y);
        }
    }

    private void drawLine(Player player, World world, RegionPoint a, RegionPoint b, double y) {
        double dx = b.x() - a.x();
        double dz = b.z() - a.z();
        double length = Math.sqrt(dx * dx + dz * dz);
        int steps = Math.max(1, (int) Math.ceil(length * 2.0D));
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            player.spawnParticle(Particle.END_ROD, new Location(world, a.x() + dx * t, y, a.z() + dz * t), 1, 0, 0, 0, 0);
        }
    }

    private static final class Session {
        private final UUID id;
        private final String name;
        private final RegionType type;
        private final int minY;
        private final int maxY;
        private final ArrayList<RegionPoint> points = new ArrayList<>();
        private String worldName;
        private boolean finished;
        private Session(UUID id, String name, RegionType type, int minY, int maxY) {
            this.id = id; this.name = name; this.type = type; this.minY = minY; this.maxY = maxY;
        }
    }
}
