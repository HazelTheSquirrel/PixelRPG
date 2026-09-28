package de.pixelrpg.rpg.guild;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.economy.Money;
import de.pixelrpg.rpg.region.PixelRegion;
import de.pixelrpg.rpg.region.RegionGeometry;
import de.pixelrpg.rpg.region.RegionManager;
import de.pixelrpg.rpg.region.RegionPoint;
import de.pixelrpg.rpg.region.RegionType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Owns the physical guild-boundary markers and turns their ordered topology into a live PixelRPG region.
 *
 * Every mutation is validated against the complete resulting polygon before it is committed.
 * Invalid edits never replace the previous valid region.
 */
public final class GuildTerritoryManager {
    private static final double EPSILON = 1.0E-7D;

    private final PixelRPGPlugin plugin;
    private final GuildManager guilds;
    private final RegionManager regions;
    private final File file;
    private final NamespacedKey markerKey;
    private final NamespacedKey guildKey;
    private final Map<UUID, TerritoryState> territories = new HashMap<>();
    private final ExecutorService persistenceExecutor;
    private boolean saveWorkerScheduled;
    private long stateRevision;
    private volatile boolean shuttingDown;

    public GuildTerritoryManager(PixelRPGPlugin plugin, GuildManager guilds, RegionManager regions) {
        this.plugin = Objects.requireNonNull(plugin);
        this.guilds = Objects.requireNonNull(guilds);
        this.regions = Objects.requireNonNull(regions);
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        this.file = new File(plugin.getDataFolder(), "guild-territories.yml");
        this.markerKey = new NamespacedKey(plugin, "guild-territory-marker");
        this.guildKey = new NamespacedKey(plugin, "guild-id");
        this.persistenceExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "PixelRPG-GuildTerritoryIO");
            thread.setDaemon(true);
            return thread;
        });
        load();
    }

    public synchronized int markerCount(UUID guildId) {
        TerritoryState state = territories.get(guildId);
        return state == null ? 0 : state.markers.size();
    }

    public synchronized double nextMarkerPrice(UUID guildId) {
        int count = markerCount(guildId);
        double base = plugin.getConfig().getDouble("economy.guild-territory.marker-base-price", 500.0D);
        double increase = plugin.getConfig().getDouble("economy.guild-territory.marker-price-increase", 500.0D);
        return Math.max(0.0D, base + Math.max(0, count - GuildTerritory.INITIAL_MARKERS) * increase);
    }

    public synchronized Optional<GuildTerritory> getTerritory(UUID guildId) {
        TerritoryState state = territories.get(guildId);
        return state == null ? Optional.empty() : Optional.of(state.snapshot());
    }

    public synchronized boolean isMarkerItem(ItemStack item) {
        return markerGuildId(item).isPresent();
    }

    public synchronized Optional<UUID> markerGuildId(ItemStack item) {
        if (item == null || item.getType() != Material.COPPER_TORCH || !item.hasItemMeta()) return Optional.empty();
        String value = item.getItemMeta().getPersistentDataContainer().get(guildKey, PersistentDataType.STRING);
        if (value == null) return Optional.empty();
        try {
            UUID guildId = UUID.fromString(value);
            return item.getItemMeta().getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE)
                    ? Optional.of(guildId) : Optional.empty();
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public synchronized ItemStack createMarkerItem(UUID guildId, int amount) {
        if (guildId == null || amount <= 0) throw new IllegalArgumentException("Invalid guild marker item");
        ItemStack item = new ItemStack(Material.COPPER_TORCH, amount);
        item.editMeta(meta -> {
            meta.displayName(Component.text("Gilden-Grenzmarker", NamedTextColor.GOLD));
            meta.lore(List.of(
                    Component.text("Kupferfackel zur Abgrenzung des Gildengebiets.", NamedTextColor.GRAY),
                    Component.text("Maximal 32 Blöcke zwischen benachbarten Markern.", NamedTextColor.GRAY)
            ));
            meta.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(guildKey, PersistentDataType.STRING, guildId.toString());
        });
        return item;
    }

    /** Grants the four initial physical boundary markers to a newly created guild leader. */
    public synchronized void grantInitialMarkers(Player player, UUID guildId) {
        if (player == null || guildId == null) return;
        giveItems(player, createMarkerItem(guildId, GuildTerritory.INITIAL_MARKERS));
    }

    /** Charges the guild treasury and gives one additional physical boundary marker to the authorized player. */
    public synchronized OperationResult purchaseMarker(Player player) {
        if (shuttingDown || player == null) return OperationResult.failure("Der Grenzmarker konnte nicht gekauft werden.");
        Guild guild = guilds.getGuild(player.getUniqueId()).orElse(null);
        if (guild == null) return OperationResult.failure("Du bist in keiner Gilde.");
        if (!guild.canManageTerritory(player.getUniqueId())) return OperationResult.failure("Nur der Gildenmeister oder Stellvertreter darf Grenzmarker kaufen.");

        double price = nextMarkerPrice(guild.id());
        if (!guilds.chargeTreasury(guild.id(), price)) {
            return OperationResult.failure("Die Gildenkasse enthält nicht genügend Goldtaler.");
        }

        giveItems(player, createMarkerItem(guild.id(), 1));
        return OperationResult.success("Grenzmarker gekauft für " + formatGold(price) + " Goldtaler.");
    }

    /**
     * Validates a marker placement before Minecraft commits the physical block placement.
     * The previous valid territory is untouched when the operation is rejected.
     */
    public synchronized OperationResult placeMarker(Player player, Block block, ItemStack item) {
        if (shuttingDown || player == null || block == null) return OperationResult.failure("Ungültiger Grenzmarker.");
        UUID guildId = markerGuildId(item).orElse(null);
        if (guildId == null) return OperationResult.failure("Das ist kein gültiger Gilden-Grenzmarker.");
        Guild guild = guilds.getGuild(player.getUniqueId()).orElse(null);
        if (guild == null || !guild.id().equals(guildId)) return OperationResult.failure("Dieser Grenzmarker gehört nicht zu deiner Gilde.");
        if (!guild.canManageTerritory(player.getUniqueId())) return OperationResult.failure("Nur der Gildenmeister oder Stellvertreter darf das Gildengebiet verändern.");
        if (block.getType() != Material.COPPER_TORCH) return OperationResult.failure("Gildengrenzmarker müssen als stehende Kupferfackeln gesetzt werden.");

        World world = block.getWorld();
        String worldName = world.getName();
        if (territories.values().stream().anyMatch(state -> state.markers.stream().anyMatch(marker ->
                marker.worldName().equals(worldName) && marker.x() == block.getX() && marker.z() == block.getZ()))) {
            return OperationResult.failure("An dieser X/Z-Position befindet sich bereits ein Grenzmarker.");
        }

        PixelRegion locationRegion = regions.find(world, block.getX() + 0.5D, block.getY(), block.getZ() + 0.5D).orElse(null);
        if (locationRegion != null && !locationRegion.isGlobal()) {
            boolean sameGuildTerritory = locationRegion.type() == RegionType.GUILD_TERRITORY
                    && guildId.equals(parseGuildId(locationRegion.properties().get("guild-id")));
            if (!sameGuildTerritory) {
                return OperationResult.failure("Grenzmarker dürfen keine bestehende Region überlappen.");
            }
        }

        TerritoryState state = territories.computeIfAbsent(guildId,
                id -> new TerritoryState(id, regionId(id), new ArrayList<>()));

        GuildTerritory.Marker marker = new GuildTerritory.Marker(
                UUID.randomUUID(), worldName, block.getX(), block.getY(), block.getZ());

        if (state.markers.size() < GuildTerritory.INITIAL_MARKERS) {
            if (!state.markers.isEmpty() && state.markers.stream().noneMatch(existing -> distance(existing, marker) <= GuildTerritory.MAX_MARKER_DISTANCE + EPSILON)) {
                return OperationResult.failure("Jeder neue Grenzmarker muss höchstens 32 Blöcke von einem bestehenden Marker entfernt sein.");
            }

            ArrayList<GuildTerritory.Marker> candidate = new ArrayList<>(state.markers);
            candidate.add(marker);

            if (candidate.size() == GuildTerritory.INITIAL_MARKERS) {
                candidate = orderInitialMarkers(candidate);
                OperationResult validation = validateCompleteBoundary(guildId, candidate, state.regionId);
                if (!validation.success()) {
                    return validation;
                }
                state.markers.clear();
                state.markers.addAll(candidate);
                if (!syncRegion(guild, state)) {
                    state.markers.clear();
                    return OperationResult.failure("Das Gildengebiet konnte nicht sicher gespeichert werden.");
                }
            } else {
                state.markers.add(marker);
            }

            save();
            return OperationResult.success(state.markers.size() == GuildTerritory.INITIAL_MARKERS
                    ? "Die ersten vier Grenzmarker bilden jetzt ein gültiges Gildengebiet."
                    : "Grenzmarker gesetzt. Noch " + (GuildTerritory.INITIAL_MARKERS - state.markers.size()) + " Marker bis zum ersten Gildengebiet.");
        }

        InsertionCandidate insertion = findInsertionCandidate(guildId, state.markers, marker, state.regionId);
        if (insertion == null) {
            return OperationResult.failure("Der neue Marker kann hier nicht sicher in die bestehende Grenze eingefügt werden. Setze ihn näher an einen Grenzabschnitt und halte alle Abstände bei höchstens 32 Blöcken.");
        }

        ArrayList<GuildTerritory.Marker> previous = new ArrayList<>(state.markers);
        state.markers.clear();
        state.markers.addAll(insertion.markers());

        if (!syncRegion(guild, state)) {
            state.markers.clear();
            state.markers.addAll(previous);
            syncRegion(guild, state);
            return OperationResult.failure("Die Erweiterung wurde aus Sicherheitsgründen nicht übernommen.");
        }

        save();
        return OperationResult.success("Gildengebiet erweitert. Neuer Grenzmarker wurde sicher in die bestehende Grenze eingefügt.");
    }

    /**
     * Removes one physical marker only when the resulting contour remains a valid closed polygon.
     * The marker is returned to the authorized guild member after the block is removed.
     */
    public synchronized OperationResult removeMarker(Player player, Block block) {
        if (shuttingDown || player == null || block == null) return OperationResult.failure("Ungültiger Grenzmarker.");
        TerritoryState state = findStateAt(block);
        if (state == null) return OperationResult.failure("Hier befindet sich kein Gilden-Grenzmarker.");

        Guild guild = guilds.getGuild(player.getUniqueId()).orElse(null);
        if (guild == null || !guild.id().equals(state.guildId)) return OperationResult.failure("Dieser Grenzmarker gehört nicht zu deiner Gilde.");
        if (!guild.canManageTerritory(player.getUniqueId())) return OperationResult.failure("Nur der Gildenmeister oder Stellvertreter darf Grenzmarker entfernen.");

        int index = indexOf(state.markers, block);
        if (index < 0) return OperationResult.failure("Der Grenzmarker konnte nicht eindeutig gefunden werden.");

        ArrayList<GuildTerritory.Marker> previous = new ArrayList<>(state.markers);
        ArrayList<GuildTerritory.Marker> candidate = new ArrayList<>(state.markers);
        candidate.remove(index);

        /*
         * A guild territory exists only while at least four markers form a closed polygon.
         * Removing the fourth marker therefore deliberately tears the region down.
         * The remaining physical markers stay removable until none are left.
         */
        if (candidate.size() < GuildTerritory.INITIAL_MARKERS) {
            state.markers.clear();
            state.markers.addAll(candidate);

            regions.delete(state.regionId);

            block.setType(Material.AIR, false);
            giveItems(player, createMarkerItem(state.guildId, 1));

            if (candidate.isEmpty()) {
                territories.remove(state.guildId);
            }

            save();
            return OperationResult.success(candidate.isEmpty()
                    ? "Grenzmarker entfernt. Das Gildengebiet wurde vollständig abgebaut."
                    : "Grenzmarker entfernt. Das Gildengebiet wurde aufgelöst; die verbleibenden Grenzmarker können ebenfalls abgebaut werden.");
        }

        OperationResult validation = validateCompleteBoundary(state.guildId, candidate, state.regionId);
        if (!validation.success()) return validation;

        state.markers.clear();
        state.markers.addAll(candidate);

        if (!syncRegion(guild, state)) {
            state.markers.clear();
            state.markers.addAll(previous);
            syncRegion(guild, state);
            return OperationResult.failure("Der Marker wurde nicht entfernt, weil die bestehende Region sonst nicht sicher erhalten werden konnte.");
        }

        block.setType(Material.AIR, false);
        giveItems(player, createMarkerItem(state.guildId, 1));
        save();
        return OperationResult.success("Grenzmarker entfernt und an dich zurückgegeben.");
    }

    public synchronized boolean isMarkerBlock(Block block) {
        return block != null && findStateAt(block) != null;
    }

    public synchronized boolean isProtectedBlock(Block block) {
        if (block == null) return false;
        if (findStateAt(block) != null) return true;
        if (findStateAt(block.getRelative(0, 1, 0)) != null) return true;
        return false;
    }

    public synchronized boolean containsProtectedBlock(List<Block> blocks) {
        if (blocks == null || blocks.isEmpty()) return false;
        return blocks.stream().anyMatch(this::isProtectedBlock);
    }

    private InsertionCandidate findInsertionCandidate(UUID guildId, List<GuildTerritory.Marker> current, GuildTerritory.Marker marker, UUID currentRegionId) {
        RegionGeometry currentGeometry = geometry(current);
        if (currentGeometry == null) return null;
        if (currentGeometry.contains(marker.x() + 0.5D, marker.z() + 0.5D)) return null;

        List<InsertionCandidate> candidates = new ArrayList<>();
        for (int i = 0; i < current.size(); i++) {
            GuildTerritory.Marker a = current.get(i);
            GuildTerritory.Marker b = current.get((i + 1) % current.size());

            if (distance(a, marker) > GuildTerritory.MAX_MARKER_DISTANCE + EPSILON
                    || distance(b, marker) > GuildTerritory.MAX_MARKER_DISTANCE + EPSILON) {
                continue;
            }

            ArrayList<GuildTerritory.Marker> candidate = new ArrayList<>(current);
            candidate.add(i + 1, marker);
            OperationResult validation = validateCompleteBoundary(guildId, candidate, currentRegionId);
            if (!validation.success()) continue;

            RegionGeometry candidateGeometry = geometry(candidate);
            if (candidateGeometry == null || candidateGeometry.area() <= currentGeometry.area() + EPSILON) continue;

            double edgeDistance = pointToSegmentDistance(
                    marker.x() + 0.5D, marker.z() + 0.5D,
                    a.x() + 0.5D, a.z() + 0.5D,
                    b.x() + 0.5D, b.z() + 0.5D
            );
            candidates.add(new InsertionCandidate(candidate, edgeDistance));
        }

        return candidates.stream().min(Comparator.comparingDouble(InsertionCandidate::edgeDistance)).orElse(null);
    }

    private OperationResult validateCompleteBoundary(UUID guildId, List<GuildTerritory.Marker> markers, UUID currentRegionId) {
        if (markers.size() < GuildTerritory.INITIAL_MARKERS) {
            return OperationResult.failure("Mindestens vier Grenzmarker sind erforderlich.");
        }

        String world = markers.getFirst().worldName();
        if (markers.stream().anyMatch(marker -> !marker.worldName().equals(world))) {
            return OperationResult.failure("Ein Gildengebiet darf nur in einer Welt liegen.");
        }

        for (int i = 0; i < markers.size(); i++) {
            for (int j = i + 1; j < markers.size(); j++) {
                if (sameXZ(markers.get(i), markers.get(j))) {
                    return OperationResult.failure("Zwei Grenzmarker dürfen nicht dieselbe X/Z-Position besitzen.");
                }
            }
        }

        for (int i = 0; i < markers.size(); i++) {
            GuildTerritory.Marker a = markers.get(i);
            GuildTerritory.Marker b = markers.get((i + 1) % markers.size());
            if (distance(a, b) > GuildTerritory.MAX_MARKER_DISTANCE + EPSILON) {
                return OperationResult.failure("Zwischen zwei benachbarten Grenzmarkern dürfen höchstens 32 Blöcke liegen.");
            }
        }

        RegionGeometry.ValidationResult validation = RegionGeometry.validate(markers.stream()
                .map(marker -> new RegionPoint(marker.x() + 0.5D, marker.z() + 0.5D))
                .toList());
        if (!validation.valid()) return OperationResult.failure(validation.error());
        Guild guild = guilds.getGuildById(guildId).orElse(null);
        if (guild == null) return OperationResult.failure("Das Königreich existiert nicht mehr.");
        double maxArea = Guild.maxTerritoryArea(guild.cityLevel());
        if (validation.geometry().area() > maxArea + EPSILON) return OperationResult.failure("Das Gebiet überschreitet die für Stadtlevel " + guild.cityLevel() + " erlaubte Fläche von " + (long) maxArea + " Blöcken².");

        for (PixelRegion region : regions.all()) {
            if (currentRegionId != null && region.id().equals(currentRegionId)) continue;
            if (region.geometry() != null && region.worldName().equals(world)
                    && validation.geometry().overlaps(region.geometry())) {
                return OperationResult.failure("Das Gildengebiet würde die bestehende Region „" + region.name() + "“ überlappen.");
            }
        }

        return OperationResult.success("valid");
    }

    private RegionGeometry geometry(List<GuildTerritory.Marker> markers) {
        if (markers.size() < 3) return null;
        RegionGeometry.ValidationResult validation = RegionGeometry.validate(markers.stream()
                .map(marker -> new RegionPoint(marker.x() + 0.5D, marker.z() + 0.5D))
                .toList());
        return validation.valid() ? validation.geometry() : null;
    }

    private boolean syncRegion(Guild guild, TerritoryState state) {
        RegionGeometry geometry = geometry(state.markers);
        if (geometry == null || state.markers.size() < GuildTerritory.INITIAL_MARKERS) return true;

        World world = Bukkit.getWorld(state.markers.getFirst().worldName());
        if (world == null) return false;

        PixelRegion region = regions.get(state.regionId).orElse(null);
        if (region == null) {
            RegionGeometry.ValidationResult result = regions.create(
                    state.regionId,
                    world.getName(),
                    geometry.points(),
                    world.getMinHeight(),
                    world.getMaxHeight() - 1,
                    guild.name(),
                    RegionType.GUILD_TERRITORY
            );
            if (!result.valid()) return false;
            region = regions.get(state.regionId).orElse(null);
            if (region == null) return false;
        } else if (!regions.replaceGeometry(state.regionId, geometry)) {
            return false;
        }

        region.setName(guild.name());
        region.setType(RegionType.GUILD_TERRITORY);
        region.setDescription("Dynamisch durch die Gilde gesetztes Gebiet.");
        region.setPriority(100);
        region.setProperty("guild-id", guild.id().toString());
        region.setOwner(guild.leaderId());
        region.clearMembers();
        guilds.getMembers(guild.id()).forEach(region::addMember);
        regions.save();
        return true;
    }

    private void restoreRegion(UUID guildId, TerritoryState state) {
        if (!guilds.getGuildById(guildId).isPresent() || state.markers.size() < GuildTerritory.INITIAL_MARKERS) return;
        Guild guild = guilds.getGuildById(guildId).orElseThrow();
        OperationResult validation = validateCompleteBoundary(guildId, state.markers, state.regionId);
        if (!validation.success()) {
            plugin.getLogger().warning("Gildengebiet " + guildId + " wurde beim Laden nicht aktiviert: " + validation.message());
            return;
        }
        syncRegion(guild, state);
    }

    private ArrayList<GuildTerritory.Marker> orderInitialMarkers(List<GuildTerritory.Marker> markers) {
        double centerX = markers.stream().mapToDouble(GuildTerritory.Marker::x).average().orElse(0.0D);
        double centerZ = markers.stream().mapToDouble(GuildTerritory.Marker::z).average().orElse(0.0D);
        ArrayList<GuildTerritory.Marker> ordered = new ArrayList<>(markers);
        ordered.sort(Comparator.comparingDouble(marker -> Math.atan2(marker.z() - centerZ, marker.x() - centerX)));
        return ordered;
    }

    private TerritoryState findStateAt(Block block) {
        String world = block.getWorld().getName();
        for (TerritoryState state : territories.values()) {
            for (GuildTerritory.Marker marker : state.markers) {
                if (marker.worldName().equals(world)
                        && marker.x() == block.getX()
                        && marker.y() == block.getY()
                        && marker.z() == block.getZ()) {
                    return state;
                }
            }
        }
        return null;
    }

    private static int indexOf(List<GuildTerritory.Marker> markers, Block block) {
        for (int i = 0; i < markers.size(); i++) {
            GuildTerritory.Marker marker = markers.get(i);
            if (marker.x() == block.getX() && marker.y() == block.getY() && marker.z() == block.getZ()
                    && marker.worldName().equals(block.getWorld().getName())) return i;
        }
        return -1;
    }

    private static boolean sameXZ(GuildTerritory.Marker a, GuildTerritory.Marker b) {
        return a.worldName().equals(b.worldName()) && a.x() == b.x() && a.z() == b.z();
    }

    private static double distance(GuildTerritory.Marker a, GuildTerritory.Marker b) {
        double dx = a.x() - b.x();
        double dz = a.z() - b.z();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static double pointToSegmentDistance(double px, double pz, double ax, double az, double bx, double bz) {
        double dx = bx - ax;
        double dz = bz - az;
        double lengthSquared = dx * dx + dz * dz;
        if (lengthSquared <= EPSILON) return Math.hypot(px - ax, pz - az);
        double t = ((px - ax) * dx + (pz - az) * dz) / lengthSquared;
        t = Math.max(0.0D, Math.min(1.0D, t));
        double cx = ax + t * dx;
        double cz = az + t * dz;
        return Math.hypot(px - cx, pz - cz);
    }

    private static UUID parseGuildId(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static UUID regionId(UUID guildId) {
        return UUID.nameUUIDFromBytes(("pixelrpg:guild-territory:" + guildId).getBytes(StandardCharsets.UTF_8));
    }

    private void giveItems(Player player, ItemStack item) {
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
        leftovers.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

    private static String formatGold(double amount) {
        return String.format(java.util.Locale.ROOT, "%.2f", amount);
    }

    private synchronized void load() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("territories");
        if (root == null) return;

        for (String guildText : root.getKeys(false)) {
            try {
                UUID guildId = UUID.fromString(guildText);
                UUID regionId = UUID.fromString(root.getString(guildText + ".region-id", regionId(guildId).toString()));
                List<GuildTerritory.Marker> markers = new ArrayList<>();
                for (String index : root.getStringList(guildText + ".marker-order")) {
                    String base = "territories." + guildText + ".markers." + index;
                    UUID markerId = UUID.fromString(yaml.getString(base + ".id"));
                    String world = yaml.getString(base + ".world");
                    int x = yaml.getInt(base + ".x");
                    int y = yaml.getInt(base + ".y");
                    int z = yaml.getInt(base + ".z");
                    markers.add(new GuildTerritory.Marker(markerId, world, x, y, z));
                }
                territories.put(guildId, new TerritoryState(guildId, regionId, markers));
            } catch (Exception exception) {
                plugin.getLogger().warning("Skipping malformed guild territory " + guildText + ": " + exception.getMessage());
            }
        }

        territories.values().forEach(state -> restoreRegion(state.guildId, state));
    }

    private synchronized void save() {
        if (shuttingDown || persistenceExecutor.isShutdown()) return;
        stateRevision++;
        if (saveWorkerScheduled) return;
        saveWorkerScheduled = true;
        persistenceExecutor.execute(this::drainSaves);
    }

    private void drainSaves() {
        while (true) {
            YamlConfiguration snapshot;
            long revision;
            synchronized (this) {
                revision = stateRevision;
                snapshot = createSnapshot();
            }
            writeAtomically(snapshot);
            synchronized (this) {
                if (revision == stateRevision) {
                    saveWorkerScheduled = false;
                    return;
                }
            }
        }
    }

    private synchronized YamlConfiguration createSnapshot() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (TerritoryState state : territories.values()) {
            String base = "territories." + state.guildId;
            yaml.set(base + ".region-id", state.regionId.toString());
            yaml.set(base + ".marker-order", state.markers.stream().map(marker -> marker.id().toString()).toList());
            for (GuildTerritory.Marker marker : state.markers) {
                String markerBase = base + ".markers." + marker.id();
                yaml.set(markerBase + ".id", marker.id().toString());
                yaml.set(markerBase + ".world", marker.worldName());
                yaml.set(markerBase + ".x", marker.x());
                yaml.set(markerBase + ".y", marker.y());
                yaml.set(markerBase + ".z", marker.z());
            }
        }
        return yaml;
    }

    private void writeAtomically(YamlConfiguration snapshot) {
        Path target = file.toPath();
        Path temporary = target.resolveSibling(file.getName() + ".tmp");
        try {
            snapshot.save(temporary.toFile());
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Could not save guild-territories.yml", exception);
        }
    }

    public synchronized void removeGuildTerritory(UUID guildId) {
        TerritoryState state = territories.remove(guildId);
        if (state == null) return;
        regions.delete(state.regionId);
        save();
    }

    public synchronized void shutdown() {
        if (shuttingDown) return;
        shuttingDown = true;
        persistenceExecutor.shutdown();
        try {
            if (!persistenceExecutor.awaitTermination(10, TimeUnit.SECONDS)) persistenceExecutor.shutdownNow();
        } catch (InterruptedException exception) {
            persistenceExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        territories.clear();
    }

    public record OperationResult(boolean success, String message) {
        static OperationResult success(String message) { return new OperationResult(true, message); }
        static OperationResult failure(String message) { return new OperationResult(false, message); }
    }

    private record InsertionCandidate(List<GuildTerritory.Marker> markers, double edgeDistance) { }

    private static final class TerritoryState {
        private final UUID guildId;
        private final UUID regionId;
        private final ArrayList<GuildTerritory.Marker> markers;

        private TerritoryState(UUID guildId, UUID regionId, List<GuildTerritory.Marker> markers) {
            this.guildId = guildId;
            this.regionId = regionId;
            this.markers = new ArrayList<>(markers);
        }

        private GuildTerritory snapshot() {
            return new GuildTerritory(guildId, regionId, markers);
        }
    }
}
