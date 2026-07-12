// src/main/java/de/pixelrpg/rpg/gui/StoryGUI.java
package de.pixelrpg.rpg.gui;

import de.pixelrpg.rpg.story.StoryChapter;
import de.pixelrpg.rpg.story.StoryManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class StoryGUI extends AbstractGUI {

    private final Player viewer;
    private final StoryManager storyManager;
    private final StoryChapter chapter;
    private final int pageIndex;

    public StoryGUI(Player viewer, StoryManager storyManager, StoryChapter chapter, int pageIndex) {
        super(27, Component.text(chapter.title() + " (" + (pageIndex + 1) + "/" + chapter.dialogueLines().size() + ")", NamedTextColor.GOLD));
        this.viewer = viewer;
        this.storyManager = storyManager;
        this.chapter = chapter;
        this.pageIndex = pageIndex;
    }

    @Override
    protected void populate() {
        List<String> lines = chapter.dialogueLines();
        String currentLine = pageIndex < lines.size() ? lines.get(pageIndex) : "";

        ItemStack book = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta bookMeta = book.getItemMeta();
        bookMeta.displayName(Component.text("Story Log", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        bookMeta.lore(List.of(
                Component.text(currentLine, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false),
                Component.text(" "),
                Component.text("Click 'Continue' below to proceed.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
        ));
        book.setItemMeta(bookMeta);
        setItem(13, book);

        boolean isLastPage = pageIndex >= lines.size() - 1;

        ItemStack nextButton = new ItemStack(isLastPage ? Material.LIME_DYE : Material.ARROW);
        ItemMeta nextMeta = nextButton.getItemMeta();
        nextMeta.displayName(Component.text(isLastPage ? "Finish Chapter & Claim Reward" : "Continue Reading",
                        isLastPage ? NamedTextColor.GREEN : NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        if (isLastPage && chapter.expReward() > 0) {
            nextMeta.lore(List.of(Component.text("Reward: " + chapter.expReward() + " XP", NamedTextColor.AQUA)
                    .decoration(TextDecoration.ITALIC, false)));
        }
        nextButton.setItemMeta(nextMeta);

        setItem(22, nextButton, event -> {
            if (isLastPage) {
                boolean completed = storyManager.completeChapter(viewer, chapter);
                if (completed) {
                    viewer.sendMessage(Component.text("Chapter completed: ", NamedTextColor.GOLD)
                            .append(Component.text(chapter.title(), NamedTextColor.YELLOW)));
                    viewer.playSound(viewer.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                }
                viewer.closeInventory();
            } else {
                new StoryGUI(viewer, storyManager, chapter, pageIndex + 1).open(viewer);
            }
        });
    }
}