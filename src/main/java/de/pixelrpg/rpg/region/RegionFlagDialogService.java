package de.pixelrpg.rpg.region;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Provides the native Paper dialog used to navigate and toggle categorized region flags. */
public final class RegionFlagDialogService {
    private final RegionManager regions;

    public RegionFlagDialogService(RegionManager regions) {
        this.regions = Objects.requireNonNull(regions);
    }

    /** Opens the categorized flag menu after validating the player's edit rights. */
    public void open(Player player, PixelRegion region) {
        if (!canEdit(player, region)) {
            player.sendMessage(Component.text("Du darfst die Flags dieser Region nicht bearbeiten.", NamedTextColor.RED));
            return;
        }
        showCategories(player, region);
    }

    private void showCategories(Player player, PixelRegion region) {
        List<ActionButton> actions = new ArrayList<>();
        for (RegionFlagCategory category : RegionFlagCategory.values()) {
            List<RegionFlag> flags = RegionFlag.forCategory(category);
            if (flags.isEmpty()) continue;
            long enabled = flags.stream().filter(flag -> effectiveFlag(region, flag)).count();
            actions.add(ActionButton.builder(Component.text(category.displayName(), NamedTextColor.WHITE))
                    .tooltip(Component.text(enabled + "/" + flags.size() + " aktiviert – Kategorie öffnen", NamedTextColor.GRAY))
                    .action(io.papermc.paper.registry.data.dialog.action.DialogAction.customClick((response, audience) -> {
                        if (audience instanceof Player target) showCategory(target, region, category);
                    }, net.kyori.adventure.text.event.ClickCallback.Options.builder().uses(1).build()))
                    .width(260)
                    .build());
        }

        player.showDialog(Dialog.create(factory -> {
            DialogRegistryEntry.Builder builder = factory.empty();
            builder.base(DialogBase.builder(Component.text("PixelRPG – Region Flags", NamedTextColor.GOLD))
                    .body(List.of(
                            DialogBody.plainMessage(Component.text("Region: ", NamedTextColor.GRAY)
                                    .append(Component.text(region.name(), NamedTextColor.GOLD).decorate(TextDecoration.BOLD))),
                            DialogBody.plainMessage(Component.text("Wähle eine Kategorie. Die einzelnen Berechtigungen können unabhängig voneinander gesetzt werden.", NamedTextColor.GRAY))
                    ))
                    .canCloseWithEscape(true)
                    .afterAction(DialogBase.DialogAfterAction.CLOSE)
                    .build());
            builder.type(DialogType.multiAction(actions, null, 2));
        }));
    }

    private void showCategory(Player player, PixelRegion region, RegionFlagCategory category) {
        PixelRegion current = region.isGlobal()
                ? regions.globalRegion(region.worldName())
                : regions.get(region.id()).orElse(null);
        if (current == null || !canEdit(player, current)) return;

        List<ActionButton> actions = new ArrayList<>();
        for (RegionFlag flag : RegionFlag.forCategory(category)) {
            boolean explicit = current.isGlobal() || current.hasFlag(flag);
            boolean enabled = effectiveFlag(current, flag);
            actions.add(ActionButton.builder(flagLabel(flag, enabled, explicit))
                    .tooltip(Component.text(
                            explicit ? (enabled ? "Klicken: deaktivieren" : "Klicken: aktivieren")
                                    : "Geerbt – klicken setzt ein eigenes Flag",
                            NamedTextColor.GRAY
                    ))
                    .action(io.papermc.paper.registry.data.dialog.action.DialogAction.customClick((response, audience) -> {
                        if (!(audience instanceof Player target)) return;
                        PixelRegion refreshed = current.isGlobal()
                                ? regions.globalRegion(current.worldName())
                                : regions.get(current.id()).orElse(null);
                        if (refreshed == null || !canEdit(target, refreshed)) return;
                        boolean value = effectiveFlag(refreshed, flag);
                        if (refreshed.isGlobal()) regions.setGlobalFlag(refreshed.worldName(), flag, !value);
                        else {
                            refreshed.setFlag(flag, !value);
                            regions.save();
                        }
                        showCategory(target, refreshed, category);
                    }, net.kyori.adventure.text.event.ClickCallback.Options.builder().uses(1).build()))
                    .width(260)
                    .build());
        }

        ActionButton back = ActionButton.builder(Component.text("← Kategorien", NamedTextColor.YELLOW))
                .tooltip(Component.text("Zurück zur Kategorienübersicht", NamedTextColor.GRAY))
                .action(io.papermc.paper.registry.data.dialog.action.DialogAction.customClick((response, audience) -> {
                    if (audience instanceof Player target) showCategories(target, current);
                }, net.kyori.adventure.text.event.ClickCallback.Options.builder().uses(1).build()))
                .width(260)
                .build();
        actions.add(back);

        player.showDialog(Dialog.create(factory -> {
            DialogRegistryEntry.Builder builder = factory.empty();
            builder.base(DialogBase.builder(Component.text("Region – " + category.displayName(), NamedTextColor.GOLD))
                    .body(List.of(DialogBody.plainMessage(Component.text(
                            "Einzelne Berechtigungen unabhängig voneinander konfigurieren.", NamedTextColor.GRAY))))
                    .canCloseWithEscape(true)
                    .afterAction(DialogBase.DialogAfterAction.CLOSE)
                    .build());
            builder.type(DialogType.multiAction(actions, null, 2));
        }));
    }

    private boolean effectiveFlag(PixelRegion region, RegionFlag flag) {
        if (region.isGlobal() || region.hasFlag(flag)) return region.flag(flag);
        return regions.globalRegion(region.worldName()).flag(flag);
    }

    private static Component flagLabel(RegionFlag flag, boolean enabled, boolean explicit) {
        Component label = Component.text(enabled ? "✓ " : "✕ ", enabled ? NamedTextColor.GREEN : NamedTextColor.RED)
                .append(Component.text(flag.displayName(), NamedTextColor.WHITE))
                .append(Component.text(enabled ? " – AN" : " – AUS", enabled ? NamedTextColor.GREEN : NamedTextColor.RED));
        if (!explicit) label = label.append(Component.text(" • geerbt", NamedTextColor.DARK_GRAY));
        return label;
    }

    private static boolean canEdit(Player player, PixelRegion region) {
        if (player.hasPermission("rpg.admin")) return true;
        return !region.isGlobal() && region.isOwner(player.getUniqueId());
    }
}
