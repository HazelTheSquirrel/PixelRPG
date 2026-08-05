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

public final class StoryBookFactory {

    private static int charsPerLine = 18;
    private static int linesPerPage = 13;

    private StoryBookFactory() {
    }

    public static void load(FileConfiguration config) {
        charsPerLine = config.getInt("story.chars-per-line", charsPerLine);
        linesPerPage = config.getInt("story.lines-per-page", linesPerPage);
    }

    public static ItemStack build(StoryChapter chapter) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.title(Component.text(chapter.title()));
        meta.author(Component.text("The Guild Archives"));

        List<String> wrappedLines = new ArrayList<>();
        wrappedLines.add(chapter.title());
        wrappedLines.add("");
        wrappedLines.add("~ Chapter Log ~");
        wrappedLines.add("");

        for (String line : chapter.dialogueLines()) {
            wrappedLines.addAll(wrapText(line));
            wrappedLines.add("");
        }

        if (chapter.expReward() > 0) {
            wrappedLines.add("Reward for completion:");
            wrappedLines.add(chapter.expReward() + " XP");
        }

        List<Component> pages = buildPages(wrappedLines);
        for (Component page : pages) {
            meta.addPages(page);
        }

        book.setItemMeta(meta);
        return book;
    }

    private static List<String> wrapText(String text) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String word : text.split(" ")) {
            if (current.length() + word.length() + 1 > charsPerLine) {
                result.add(current.toString());
                current = new StringBuilder();
            }
            if (!current.isEmpty()) {
                current.append(' ');
            }
            current.append(word);
        }
        if (!current.isEmpty()) {
            result.add(current.toString());
        }
        return result;
    }

    private static List<Component> buildPages(List<String> lines) {
        List<Component> pages = new ArrayList<>();
        Component currentPage = Component.empty();
        int lineCount = 0;

        for (String line : lines) {
            NamedTextColor color = line.startsWith("~") ? NamedTextColor.DARK_GRAY
                    : line.equals("Reward for completion:") || line.endsWith("XP") ? NamedTextColor.DARK_GREEN
                    : NamedTextColor.BLACK;
            TextDecoration.State bold = line.equals(lines.get(0)) ? TextDecoration.State.TRUE : TextDecoration.State.FALSE;

            currentPage = currentPage.append(Component.text(line, color).decoration(TextDecoration.BOLD, bold))
                    .append(Component.newline());
            lineCount++;

            if (lineCount >= linesPerPage) {
                pages.add(currentPage);
                currentPage = Component.empty();
                lineCount = 0;
            }
        }

        if (lineCount > 0) {
            pages.add(currentPage);
        }
        if (pages.isEmpty()) {
            pages.add(Component.text(" "));
        }

        return pages;
    }
}