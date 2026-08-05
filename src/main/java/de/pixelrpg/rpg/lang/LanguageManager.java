package de.pixelrpg.rpg.lang;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public final class LanguageManager {

    private final Plugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<String, String> messages = new HashMap<>();
    private final Map<String, String> fallback = new HashMap<>();
    private String currentLanguage = "en";

    public LanguageManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void load(String languageCode) {
        this.currentLanguage = languageCode;
        messages.clear();
        fallback.clear();

        loadInto(fallback, "en");
        if (!languageCode.equalsIgnoreCase("en")) {
            loadInto(messages, languageCode);
        } else {
            messages.putAll(fallback);
        }
    }

    private void loadInto(Map<String, String> target, String code) {
        File file = new File(plugin.getDataFolder(), "lang/" + code + ".yml");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try (InputStream in = plugin.getResource("lang/" + code + ".yml")) {
                if (in != null) {
                    Files.copy(in, file.toPath());
                }
            } catch (IOException ignored) {
            }
        }
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        flatten(yaml, "", target);
    }

    private void flatten(ConfigurationSection section, String path, Map<String, String> target) {
        for (String key : section.getKeys(false)) {
            String fullKey = path.isEmpty() ? key : path + "." + key;
            if (section.isConfigurationSection(key)) {
                flatten(section.getConfigurationSection(key), fullKey, target);
            } else {
                target.put(fullKey, section.getString(key, ""));
            }
        }
    }

    /**
     * Liefert die gerenderte Komponente für einen Sprachkey, mit optionalen
     * Platzhalter-Paaren (%key% -> value). Fällt auf Englisch zurück, falls der
     * Key in der aktiven Sprache fehlt, und auf den rohen Key, falls er auch
     * dort fehlt (verhindert stumme Leerausgaben bei fehlenden Übersetzungen).
     */
    public Component get(String key, String... placeholders) {
        String raw = resolveRaw(key);
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            raw = raw.replace("%" + placeholders[i] + "%", placeholders[i + 1]);
        }
        return miniMessage.deserialize(raw);
    }

    private String resolveRaw(String key) {
        String raw = messages.get(key);
        if (raw != null) {
            return raw;
        }
        raw = fallback.get(key);
        if (raw != null) {
            return raw;
        }
        return key;
    }

    public Component prefixed(String key, String... placeholders) {
        String prefixRaw = messages.getOrDefault("prefix", fallback.getOrDefault("prefix", ""));
        return miniMessage.deserialize(prefixRaw).append(get(key, placeholders));
    }

    /**
     * Sendet die übersetzte Nachricht direkt an den Spieler. Bevorzugter
     * Einstiegspunkt für sämtliche Spieler-Feedback-Nachrichten im Plugin.
     */
    public void send(Player player, String key, String... placeholders) {
        player.sendMessage(get(key, placeholders));
    }

    public void sendPrefixed(Player player, String key, String... placeholders) {
        player.sendMessage(prefixed(key, placeholders));
    }

    public void sendActionBar(Player player, String key, String... placeholders) {
        player.sendActionBar(get(key, placeholders));
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }
}