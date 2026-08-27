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
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class LanguageManager {

    private static final String DEFAULT_LANGUAGE = "en";
    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("en", "de", "fr", "es");

    private final Plugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<String, Map<String, String>> languages = new HashMap<>();
    private String currentLanguage = DEFAULT_LANGUAGE;

    public LanguageManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void load(String languageCode) {
        currentLanguage = normalizeLanguage(languageCode);
        languages.clear();

        for (String supportedLanguage : SUPPORTED_LANGUAGES) {
            Map<String, String> messages = new HashMap<>();
            loadInto(messages, supportedLanguage);
            languages.put(supportedLanguage, messages);
        }

        if (languages.getOrDefault(DEFAULT_LANGUAGE, Map.of()).isEmpty()) {
            plugin.getLogger().warning("English language file is missing or empty; translation keys will be shown as fallback text.");
        }

        if (!SUPPORTED_LANGUAGES.contains(currentLanguage)) {
            currentLanguage = DEFAULT_LANGUAGE;
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
            } catch (IOException exception) {
                plugin.getLogger().warning("Could not create language file " + code + ".yml: " + exception.getMessage());
            }
        }

        if (!file.exists()) {
            plugin.getLogger().warning("Language file not found: lang/" + code + ".yml");
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
     * Resolves a translation for the server's configured default language.
     * This overload remains available for non-player contexts such as logs or
     * server-side content generation.
     */
    public Component get(String key, String... placeholders) {
        return render(currentLanguage, key, placeholders);
    }

    /**
     * Resolves a translation using the language selected in the player's
     * Minecraft client. Unsupported client locales fall back to English.
     */
    public Component get(Player player, String key, String... placeholders) {
        return render(resolvePlayerLanguage(player), key, placeholders);
    }

    private Component render(String language, String key, String... placeholders) {
        String raw = resolveRaw(language, key);
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            raw = raw.replace("%" + placeholders[i] + "%", placeholders[i + 1]);
        }
        return miniMessage.deserialize(raw);
    }

    private String resolveRaw(String language, String key) {
        Map<String, String> selected = languages.get(language);
        if (selected != null) {
            String raw = selected.get(key);
            if (raw != null) {
                return raw;
            }
        }

        Map<String, String> english = languages.get(DEFAULT_LANGUAGE);
        if (english != null) {
            String raw = english.get(key);
            if (raw != null) {
                return raw;
            }
        }

        return key;
    }

    public Component prefixed(String key, String... placeholders) {
        return prefix(currentLanguage).append(get(key, placeholders));
    }

    public Component prefixed(Player player, String key, String... placeholders) {
        String language = resolvePlayerLanguage(player);
        return prefix(language).append(render(language, key, placeholders));
    }

    private Component prefix(String language) {
        String raw = resolveRaw(language, "prefix");
        return miniMessage.deserialize(raw);
    }

    /**
     * Sends a translated message using the player's current Minecraft client language.
     */
    public void send(Player player, String key, String... placeholders) {
        player.sendMessage(get(player, key, placeholders));
    }

    /**
     * Sends a translated message with the translated prefix using the player's locale.
     */
    public void sendPrefixed(Player player, String key, String... placeholders) {
        player.sendMessage(prefixed(player, key, placeholders));
    }

    /**
     * Sends an action bar using the player's current Minecraft client language.
     */
    public void sendActionBar(Player player, String key, String... placeholders) {
        player.sendActionBar(get(player, key, placeholders));
    }

    public String resolvePlayerLanguage(Player player) {
        if (player == null) {
            return currentLanguage;
        }

        Locale locale = player.locale();
        String language = locale == null ? DEFAULT_LANGUAGE : locale.getLanguage();
        return SUPPORTED_LANGUAGES.contains(language.toLowerCase(Locale.ROOT))
                ? language.toLowerCase(Locale.ROOT)
                : DEFAULT_LANGUAGE;
    }

    private String normalizeLanguage(String languageCode) {
        if (languageCode == null || languageCode.isBlank()) {
            return DEFAULT_LANGUAGE;
        }
        String normalized = languageCode.toLowerCase(Locale.ROOT).replace('-', '_');
        String baseLanguage = normalized.contains("_")
                ? normalized.substring(0, normalized.indexOf('_'))
                : normalized;
        return SUPPORTED_LANGUAGES.contains(baseLanguage) ? baseLanguage : DEFAULT_LANGUAGE;
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }

    public Set<String> getSupportedLanguages() {
        return SUPPORTED_LANGUAGES;
    }
}