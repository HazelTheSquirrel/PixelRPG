package de.pixelrpg.rpg.region;

import de.pixelrpg.rpg.dialogue.DialogueEngineCloseButton;
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
import java.util.UUID;

/** Provides the native Paper dialog used to toggle region flags one by one. */
public final class RegionFlagDialogService {
    private final RegionManager regions;

    public RegionFlagDialogService(RegionManager regions) {
        this.regions = Objects.requireNonNull(regions);
    }

    /** Opens the flag list for a region after validating the player's edit rights. */
    public void open(Player player, PixelRegion region) {
        if (!canEdit(player, region)) {
            player.sendMessage(Component.text("Du darfst die Flags dieser Region nicht bearbeiten.", NamedTextColor.RED));
            return;
        }
        show(player, region);
    }

    private void show(Player player, PixelRegion region) {
        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text("Region: ", NamedTextColor.GRAY)
                        .append(Component.text(region.name(), NamedTextColor.GOLD).decorate(TextDecoration.BOLD))),
                DialogBody.plainMessage(Component.text("Klicke auf ein Flag, um es direkt umzuschalten. Änderungen werden sofort gespeichert.", NamedTextColor.GRAY))
        );
        List<ActionButton> actions = new ArrayList<>();
        for (RegionFlag flag : RegionFlag.values()) {
            boolean enabled = region.flag(flag);
            actions.add(ActionButton.builder(flagLabel(flag, enabled))
                    .tooltip(Component.text(enabled ? "Klicken: deaktivieren" : "Klicken: aktivieren", NamedTextColor.GRAY))
                    .action(io.papermc.paper.registry.data.dialog.action.DialogAction.customClick((response, audience) -> {
                        if (!(audience instanceof Player target)) return;
                        UUID regionId = region.id();
                        PixelRegion current = region.isGlobal()
                                ? regions.globalRegion(region.worldName())
                                : regions.get(regionId).orElse(null);
                        if (current == null || !canEdit(target, current)) {
                            target.sendMessage(Component.text("Du darfst diese Region nicht mehr bearbeiten.", NamedTextColor.RED));
                            return;
                        }
                        if (current.isGlobal()) regions.setGlobalFlag(current.worldName(), flag, !current.flag(flag));
                        else {
                            current.setFlag(flag, !current.flag(flag));
                            regions.save();
                        }
                        PixelRegion refreshed = current.isGlobal() ? regions.globalRegion(current.worldName()) : regions.get(regionId).orElse(current);
                        show(target, refreshed);
                    }, net.kyori.adventure.text.event.ClickCallback.Options.builder().uses(1).build()))
                    .width(260)
                    .build());
        }
        player.showDialog(Dialog.create(factory -> {
            DialogRegistryEntry.Builder builder = factory.empty();
            builder.base(DialogBase.builder(Component.text("PixelRPG – Region Flags", NamedTextColor.GOLD))
                    .body(body)
                    .canCloseWithEscape(true)
                    .afterAction(DialogBase.DialogAfterAction.CLOSE)
                    .build());
            builder.type(DialogType.multiAction(actions, DialogueEngineCloseButton.create(), 2));
        }));
    }

    private static Component flagLabel(RegionFlag flag, boolean enabled) {
        return Component.text(enabled ? "✓ " : "✕ ", enabled ? NamedTextColor.GREEN : NamedTextColor.RED)
                .append(Component.text(flag.displayName(), NamedTextColor.WHITE))
                .append(Component.text(enabled ? " – AN" : " – AUS", enabled ? NamedTextColor.GREEN : NamedTextColor.RED));
    }

    private static boolean canEdit(Player player, PixelRegion region) {
        return player.hasPermission("rpg.admin") || region.isGlobal() ? player.hasPermission("rpg.admin") : region.isOwner(player.getUniqueId());
    }
}
