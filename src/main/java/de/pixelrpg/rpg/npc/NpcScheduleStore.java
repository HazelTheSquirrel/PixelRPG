package de.pixelrpg.rpg.npc;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

public final class NpcScheduleStore {
    private final Plugin plugin;
    private final File file;
    private final Gson gson = new Gson();
    private Map<String, Map<String, ScheduleTarget>> schedules = Map.of();

    public NpcScheduleStore(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.file = new File(plugin.getDataFolder(), "npc-schedules.json");
    }

    public void load() {
        copyDefault();
        if (!file.exists()) return;
        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            Map<String, Map<String, ScheduleTarget>> loaded = gson.fromJson(reader,
                    new TypeToken<Map<String, Map<String, ScheduleTarget>>>() { }.getType());
            schedules = loaded == null ? Map.of() : Map.copyOf(loaded);
        } catch (IOException | RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to load npc-schedules.json", exception);
        }
    }

    public Location resolve(String schedule, String activity, Location home) {
        Map<String, ScheduleTarget> definition = schedules.get(schedule);
        if (definition == null) definition = schedules.get("resident");
        ScheduleTarget target = definition == null ? null : definition.get(activity);
        if (target == null) return home.clone();
        return home.clone().add(target.x(), target.y(), target.z());
    }

    private void copyDefault() {
        if (file.exists()) return;
        try {
            Files.createDirectories(file.getParentFile().toPath());
            plugin.saveResource("data/npc-schedules.json", false);
            File source = new File(plugin.getDataFolder(), "data/npc-schedules.json");
            if (source.exists()) Files.copy(source.toPath(), file.toPath());
        } catch (IOException | IllegalArgumentException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to create default npc-schedules.json", exception);
        }
    }

    public record ScheduleTarget(double x, double y, double z) { }
}
