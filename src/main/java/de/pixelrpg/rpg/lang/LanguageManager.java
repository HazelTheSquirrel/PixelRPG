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
            "en", Map.ofEntries(
                    Map.entry("common.close", "Close"), Map.entry("common.cancel", "Cancel"),
                    Map.entry("quest.max-active", "You can have at most %max% active quests."),
                    Map.entry("quest.objective.hunt", "Kill %amount%x"), Map.entry("quest.objective.collect", "Collect %amount%x"),
                    Map.entry("quest.objective.talk", "Talk to %target%"), Map.entry("quest.objective.reach", "Reach the designated destination."),
                    Map.entry("quest.objective.global", "Participate in the server-wide objective: %target%"),
                    Map.entry("quest.objective-label", "Objective: "), Map.entry("quest.progress-label", " • Progress: "),
                    Map.entry("quest.required-item", "Required: %amount%x"), Map.entry("quest.detail-hint", "Quest objective and current progress are shown here at any time."),
                    Map.entry("quest.list-hint", "Click for details."), Map.entry("quest.abandon-desc", "Current progress will be lost."),
                    Map.entry("companion.unlocked", "Companion unlocked: %name%")
            ),
            "de", Map.ofEntries(
                    Map.entry("common.close", "Schließen"), Map.entry("common.cancel", "Abbrechen"),
                    Map.entry("quest.max-active", "Du kannst maximal %max% Quests gleichzeitig aktiv haben."),
                    Map.entry("quest.objective.hunt", "Töte %amount%x"), Map.entry("quest.objective.collect", "Sammle %amount%x"),
                    Map.entry("quest.objective.talk", "Sprich mit %target%"), Map.entry("quest.objective.reach", "Erreiche den angegebenen Zielort."),
                    Map.entry("quest.objective.global", "Beteilige dich am serverweiten Ziel: %target%"),
                    Map.entry("quest.objective-label", "Ziel: "), Map.entry("quest.progress-label", " • Fortschritt: "),
                    Map.entry("quest.required-item", "Benötigt: %amount%x"), Map.entry("quest.detail-hint", "Questziel und aktueller Fortschritt sind jederzeit hier sichtbar."),
                    Map.entry("quest.list-hint", "Für Details klicken."), Map.entry("quest.abandon-desc", "Der aktuelle Fortschritt geht verloren."),
                    Map.entry("companion.unlocked", "Begleiter freigeschaltet: %name%")
            ),
            "fr", Map.ofEntries(
                    Map.entry("common.close", "Fermer"), Map.entry("common.cancel", "Annuler"),
                    Map.entry("quest.max-active", "Vous pouvez avoir au maximum %max% quêtes actives."),
                    Map.entry("quest.objective.hunt", "Tuez %amount%x"), Map.entry("quest.objective.collect", "Collectez %amount%x"),
                    Map.entry("quest.objective.talk", "Parlez à %target%"), Map.entry("quest.objective.reach", "Atteignez la destination indiquée."),
                    Map.entry("quest.objective.global", "Participez à l'objectif serveur : %target%"),
                    Map.entry("quest.objective-label", "Objectif : "), Map.entry("quest.progress-label", " • Progression : "),
                    Map.entry("quest.required-item", "Requis : %amount%x"), Map.entry("quest.detail-hint", "L'objectif et la progression de la quête sont visibles ici à tout moment."),
                    Map.entry("quest.list-hint", "Cliquez pour les détails."), Map.entry("quest.abandon-desc", "La progression actuelle sera perdue."),
                    Map.entry("companion.unlocked", "Compagnon débloqué : %name%")
            ),
            "es", Map.ofEntries(
                    Map.entry("common.close", "Cerrar"), Map.entry("common.cancel", "Cancelar"),
                    Map.entry("quest.max-active", "Puedes tener como máximo %max% misiones activas."),
                    Map.entry("quest.objective.hunt", "Mata %amount%x"), Map.entry("quest.objective.collect", "Recolecta %amount%x"),
                    Map.entry("quest.objective.talk", "Habla con %target%"), Map.entry("quest.objective.reach", "Alcanza el destino indicado."),
                    Map.entry("quest.objective.global", "Participa en el objetivo del servidor: %target%"),
                    Map.entry("quest.objective-label", "Objetivo: "), Map.entry("quest.progress-label", " • Progreso: "),
                    Map.entry("quest.required-item", "Necesario: %amount%x"), Map.entry("quest.detail-hint", "El objetivo y el progreso actual de la misión se muestran aquí."),
                    Map.entry("quest.list-hint", "Haz clic para ver los detalles."), Map.entry("quest.abandon-desc", "Se perderá el progreso actual."),
                    Map.entry("companion.unlocked", "Compañero desbloqueado: %name%")
            )
    );

    private final Plugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<String, Map<String, String>> languages = new LinkedHashMap<>();
    private final Set<String> invalidTranslations = new HashSet<>();
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

    /** Returns the raw localized string for advanced rendering and content fallbacks. */
    public String raw(String key) {
        return resolveRaw(currentLanguage, key);
    }

    /** Returns the raw localized string for the player's Minecraft client language. */
    public String raw(Player player, String key) {
        return resolveRaw(resolvePlayerLanguage(player), key);
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
