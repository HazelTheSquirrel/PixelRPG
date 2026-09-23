package de.pixelrpg.rpg.npc;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;

public final class YamlNpcRepository implements NpcRepository {
    private final Plugin plugin;
    private final Path file;
    private final ExecutorService executor;

    public YamlNpcRepository(Plugin plugin, ExecutorService executor) {
        this.plugin = plugin;
        this.file = plugin.getDataFolder().toPath().resolve("npcs.yml");
        this.executor = executor;
    }

    @Override
    public CompletableFuture<List<NpcRecord>> load() {
        return CompletableFuture.supplyAsync(() -> {
            if (!Files.exists(file)) return List.of();
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file.toFile());
            ConfigurationSection root = yaml.getConfigurationSection("npcs");
            if (root == null) return List.of();
            List<NpcRecord> result = new ArrayList<>();
            for (String id : root.getKeys(false)) {
                ConfigurationSection section = root.getConfigurationSection(id);
                if (section == null) continue;
                result.add(new NpcRecord(
                        id,
                        section.getString("type"),
                        section.getString("name", "NPC"),
                        section.getString("world"),
                        section.getDouble("x"),
                        section.getDouble("y"),
                        section.getDouble("z"),
                        (float) section.getDouble("yaw"),
                        (float) section.getDouble("pitch"),
                        section.getString("skin-source"),
                        section.getString("skin-value"),
                        section.getString("skin-signature"),
                        section.getString("profession")));
            }
            return List.copyOf(result);
        }, executor);
    }

    public CompletableFuture<Integer> loadNextId() {
        return CompletableFuture.supplyAsync(() -> {
            if (!Files.exists(file)) return 1;
            return Math.max(1, YamlConfiguration.loadConfiguration(file.toFile()).getInt("next-id", 1));
        }, executor);
    }

    @Override
    public void close() {
        executor.shutdown();
    }

    @Override
    public CompletableFuture<Void> save(int nextId, List<NpcRecord> records) {
        List<NpcRecord> snapshot = List.copyOf(records);
        return CompletableFuture.runAsync(() -> {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set("next-id", nextId);
            for (NpcRecord npc : snapshot) {
                String path = "npcs." + npc.id();
                yaml.set(path + ".type", npc.type());
                yaml.set(path + ".name", npc.name());
                yaml.set(path + ".world", npc.world());
                yaml.set(path + ".x", npc.x());
                yaml.set(path + ".y", npc.y());
                yaml.set(path + ".z", npc.z());
                yaml.set(path + ".yaw", (double) npc.yaw());
                yaml.set(path + ".pitch", (double) npc.pitch());
                if (npc.skinSource() != null && !npc.skinSource().isBlank()) yaml.set(path + ".skin-source", npc.skinSource());
                if (npc.skinValue() != null && !npc.skinValue().isBlank()) yaml.set(path + ".skin-value", npc.skinValue());
                if (npc.skinSignature() != null && !npc.skinSignature().isBlank()) yaml.set(path + ".skin-signature", npc.skinSignature());
                if (npc.profession() != null) yaml.set(path + ".profession", npc.profession());
            }
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            try {
                Files.createDirectories(file.getParent());
                yaml.save(temporary.toFile());
                try {
                    Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException unsupported) {
                    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException exception) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) {}
                throw new IllegalStateException("Failed to save NPC persistence", exception);
            }
        }, executor);
    }
}
