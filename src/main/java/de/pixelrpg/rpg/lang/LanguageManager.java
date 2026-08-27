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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LanguageManager {
    private static final String DEFAULT_LANGUAGE = "en";
    private static final Set<String> KNOWN_LANGUAGES = Set.of("en", "de", "fr", "es");
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%([A-Za-z0-9_.-]+)%");
    private static final Map<String, Map<String, String>> BUILTIN_MESSAGES = Map.of(
            "en", Map.of("common.close", "Close", "common.cancel", "Cancel"),
            "de", Map.of("common.close", "Schließen", "common.cancel", "Abbrechen"),
            "fr", Map.of("common.close", "Fermer", "common.cancel", "Annuler"),
            "es", Map.of("common.close", "Cerrar", "common.cancel", "Cancelar")
    );

    private final Plugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<String, Map<String, String>> languages = new LinkedHashMap<>();
    private final Set<String> invalidTranslations = new java.util.HashSet<>();
    private Set<String> supportedLanguages = Set.of(DEFAULT_LANGUAGE);
    private String currentLanguage = DEFAULT_LANGUAGE;

    public LanguageManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void load(String languageCode) {
        currentLanguage = normalizeLanguage(languageCode);
        languages.clear();
        invalidTranslations.clear();

        LinkedHashSet<String> configuredLanguages = new LinkedHashSet<>();
        for (String configured : plugin.getConfig().getStringList("language.supported")) {
            String normalized = normalizeLanguage(configured);
            if (KNOWN_LANGUAGES.contains(normalized)) configuredLanguages.add(normalized);
        }
        configuredLanguages.add(DEFAULT_LANGUAGE);
        supportedLanguages = Set.copyOf(configuredLanguages);

        for (String supportedLanguage : supportedLanguages) {
            Map<String, String> messages = new LinkedHashMap<>();
            loadInto(messages, supportedLanguage);
            languages.put(supportedLanguage, Map.copyOf(messages));
        }

        validateLanguageFiles();

        if (languages.getOrDefault(DEFAULT_LANGUAGE, Map.of()).isEmpty()) {
            plugin.getLogger().warning("English language file is missing or empty; translation keys will be used as fallback text.");
        }
        if (!supportedLanguages.contains(currentLanguage)) currentLanguage = DEFAULT_LANGUAGE;
    }

    private void loadInto(Map<String, String> target, String code) {
        File file = new File(plugin.getDataFolder(), "lang/" + code + ".yml");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try (InputStream in = plugin.getResource("lang/" + code + ".yml")) {
                if (in != null) Files.copy(in, file.toPath());
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
                ConfigurationSection child = section.getConfigurationSection(key);
                if (child != null) flatten(child, fullKey, target);
            } else {
                target.put(fullKey, section.getString(key, ""));
            }
        }
    }

    private void validateLanguageFiles() {
        Map<String, String> english = languages.getOrDefault(DEFAULT_LANGUAGE, Map.of());
        for (String language : supportedLanguages) {
            Map<String, String> selected = languages.getOrDefault(language, Map.of());
            if (!DEFAULT_LANGUAGE.equals(language)) {
                Set<String> missing = new HashSet<>(english.keySet());
                missing.removeAll(selected.keySet());
                if (!missing.isEmpty()) {
                    plugin.getLogger().warning("Language " + language + " is missing " + missing.size() + " translation key(s); English fallback will be used.");
                }
            }
            validatePlaceholders(language, english, selected);
        }
    }

    private void validatePlaceholders(String language, Map<String, String> reference, Map<String, String> selected) {
        for (Map.Entry<String, String> entry : reference.entrySet()) {
            String selectedText = selected.get(entry.getKey());
            if (selectedText == null) continue;
            Set<String> expected = placeholders(entry.getValue());
            Set<String> actual = placeholders(selectedText);
            if (!expected.equals(actual)) {
                invalidTranslations.add(language + ':' + entry.getKey());
                plugin.getLogger().warning("Language " + language + " has mismatched placeholders for key " + entry.getKey()
                        + " (expected " + expected + ", found " + actual + "); English fallback will be used.");
            }
        }
    }

    private Set<String> placeholders(String value) {
        Set<String> result = new HashSet<>();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(value == null ? "" : value);
        while (matcher.find()) result.add(matcher.group(1));
        return result;
    }

    public Component get(String key, String... placeholders) {
        return render(currentLanguage, key, placeholders);
    }

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
        if (!invalidTranslations.contains(language + ':' + key)) {
            Map<String, String> selected = languages.get(language);
            if (selected != null) {
                String raw = selected.get(key);
                if (raw != null) return raw;
            }
        }
        if (!DEFAULT_LANGUAGE.equals(language) && !invalidTranslations.contains(DEFAULT_LANGUAGE + ':' + key)) {
            Map<String, String> english = languages.get(DEFAULT_LANGUAGE);
            if (english != null) {
                String raw = english.get(key);
                if (raw != null) return raw;
            }
        }
        return BUILTIN_MESSAGES.getOrDefault(language, BUILTIN_MESSAGES.get(DEFAULT_LANGUAGE)).getOrDefault(key, key);
    }

    public Component prefixed(String key, String... placeholders) {
        return prefix(currentLanguage).append(get(key, placeholders));
    }

    public Component prefixed(Player player, String key, String... placeholders) {
        String language = resolvePlayerLanguage(player);
        return prefix(language).append(render(language, key, placeholders));
    }

    private Component prefix(String language) {
        return miniMessage.deserialize(resolveRaw(language, "prefix"));
    }

    /** Sends a translated message using the player's current Minecraft client language. */
    public void send(Player player, String key, String... placeholders) {
        player.sendMessage(get(player, key, placeholders));
    }

    /** Sends a translated message with the translated prefix using the player's locale. */
    public void sendPrefixed(Player player, String key, String... placeholders) {
        player.sendMessage(prefixed(player, key, placeholders));
    }

    /** Sends an action bar using the player's current Minecraft client language. */
    public void sendActionBar(Player player, String key, String... placeholders) {
        player.sendActionBar(get(player, key, placeholders));
    }

    public String resolvePlayerLanguage(Player player) {
        if (player == null) return currentLanguage;
        Locale locale = player.locale();
        String language = locale == null ? DEFAULT_LANGUAGE : locale.getLanguage().toLowerCase(Locale.ROOT);
        return supportedLanguages.contains(language) ? language : DEFAULT_LANGUAGE;
    }

    private String normalizeLanguage(String languageCode) {
        if (languageCode == null || languageCode.isBlank()) return DEFAULT_LANGUAGE;
        String normalized = languageCode.toLowerCase(Locale.ROOT).replace('-', '_');
        String baseLanguage = normalized.contains("_") ? normalized.substring(0, normalized.indexOf('_')) : normalized;
        return KNOWN_LANGUAGES.contains(baseLanguage) ? baseLanguage : DEFAULT_LANGUAGE;
    }

    public String getCurrentLanguage() { return currentLanguage; }
    public Set<String> getSupportedLanguages() { return supportedLanguages; }
}
