package de.pixelrpg.rpg.story;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.List;

/** Creates the reference story book presentation from chapter content. */
public final class StoryBookFactory {
    private static int charsPerLine = 18;
    private static int linesPerPage = 13;

    private StoryBookFactory() {}

    public static void load(FileConfiguration config) {
        charsPerLine = Math.max(1, config.getInt("story.chars-per-line", charsPerLine));
        linesPerPage = Math.max(1, config.getInt("story.lines-per-page", linesPerPage));
    }

    public static ItemStack build(StoryChapter chapter) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.title(Component.text(chapter.title()));
        meta.author(Component.text("The Guild Archives"));

        List<String> lines = new ArrayList<>();
        lines.add(chapter.title());
        lines.add("");
        lines.add("~ Chapter Log ~");
        lines.add("");
        for (String line : chapter.dialogueLines()) {
            lines.addAll(wrap(line));
            lines.add("");
        }
        if (chapter.expReward() > 0) {
            lines.add("Reward for completion:");
            lines.add(chapter.expReward() + " XP");
        }

        for (Component page : pages(lines)) meta.addPages(page);
        book.setItemMeta(meta);
        return book;
    }

    private static List<String> wrap(String text) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            if (current.length() + word.length() + 1 > charsPerLine && !current.isEmpty()) {
                result.add(current.toString());
                current.setLength(0);
            }
            if (!current.isEmpty()) current.append(' ');
            current.append(word);
        }
        if (!current.isEmpty()) result.add(current.toString());
        return result;
    }

    private static List<Component> pages(List<String> lines) {
        List<Component> pages = new ArrayList<>();
        Component current = Component.empty();
        int count = 0;
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            NamedTextColor color = line.startsWith("~") ? NamedTextColor.DARK_GRAY
                    : line.equals("Reward for completion:") || line.endsWith("XP") ? NamedTextColor.DARK_GREEN
                    : NamedTextColor.BLACK;
            TextDecoration.State bold = index == 0 ? TextDecoration.State.TRUE : TextDecoration.State.FALSE;
            current = current.append(Component.text(line, color).decoration(TextDecoration.BOLD, bold)).append(Component.newline());
            if (++count >= linesPerPage) {
                pages.add(current);
                current = Component.empty();
                count = 0;
            }
        }
        if (count > 0) pages.add(current);
        if (pages.isEmpty()) pages.add(Component.text(" "));
        return pages;
    }
}
