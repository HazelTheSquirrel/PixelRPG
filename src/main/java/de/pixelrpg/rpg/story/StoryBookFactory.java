package de.pixelrpg.rpg.story;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.List;

public final class StoryBookFactory {
    private final int charsPerLine;
    private final int linesPerPage;

    public StoryBookFactory(int charsPerLine, int linesPerPage) {
        if (charsPerLine < 1) throw new IllegalArgumentException("charsPerLine must be positive");
        if (linesPerPage < 1) throw new IllegalArgumentException("linesPerPage must be positive");
        this.charsPerLine = charsPerLine;
        this.linesPerPage = linesPerPage;
    }

    public ItemStack build(StoryChapter chapter) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.title(Component.text(chapter.title()));
        meta.author(Component.text("The Guild Archives"));

        List<String> lines = new ArrayList<>();
        lines.add(chapter.title());
        lines.add("");
        lines.add("~ Chapter Log ~");
        lines.add("");

        for (String dialogueLine : chapter.dialogueLines()) {
            lines.addAll(wrapText(dialogueLine));
            lines.add("");
        }

        if (chapter.expReward() > 0L) {
            lines.add("Reward for completion:");
            lines.add(chapter.expReward() + " XP");
        }

        for (Component page : buildPages(lines)) {
            meta.addPages(page);
        }

        book.setItemMeta(meta);
        return book;
    }

    private List<String> wrapText(String text) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String word : text.split(" ")) {
            if (current.length() > 0 && current.length() + word.length() + 1 > charsPerLine) {
                result.add(current.toString());
                current.setLength(0);
            }
            if (current.length() > 0) current.append(' ');
            current.append(word);
        }

        if (current.length() > 0) result.add(current.toString());
        return result;
    }

    private List<Component> buildPages(List<String> lines) {
        List<Component> pages = new ArrayList<>();
        Component currentPage = Component.empty();
        int lineCount = 0;

        for (String line : lines) {
            NamedTextColor color = line.startsWith("~")
                    ? NamedTextColor.DARK_GRAY
                    : line.equals("Reward for completion:") || line.endsWith("XP")
                    ? NamedTextColor.DARK_GREEN
                    : NamedTextColor.BLACK;
            TextDecoration.State bold = line.equals(lines.getFirst())
                    ? TextDecoration.State.TRUE
                    : TextDecoration.State.FALSE;

            currentPage = currentPage
                    .append(Component.text(line, color).decoration(TextDecoration.BOLD, bold))
                    .append(Component.newline());
            lineCount++;

            if (lineCount >= linesPerPage) {
                pages.add(currentPage);
                currentPage = Component.empty();
                lineCount = 0;
            }
        }

        if (lineCount > 0) pages.add(currentPage);
        if (pages.isEmpty()) pages.add(Component.text(" "));
        return pages;
    }
}
