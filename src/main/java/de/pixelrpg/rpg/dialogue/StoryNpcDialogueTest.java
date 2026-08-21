// src/main/java/de/pixelrpg/rpg/dialogue/StoryNpcDialogueTest.java (VOLLSTÄNDIG, ersetzt alte Datei — Handwerk: ein Button pro Rezept mit Item+Material+Dauer in einer Zeile statt Body-Anzeige + separatem Herstellen-Button, echter Scheduler-Delay statt Sofort-Ergebnis)
package de.pixelrpg.rpg.dialogue;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.gui.ShopGUI;
import de.pixelrpg.rpg.item.ItemRarity;
import de.pixelrpg.rpg.item.RPGItemBuilder;
import de.pixelrpg.rpg.npc.NpcManager;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.player.AttributeConfig;
import de.pixelrpg.rpg.player.PlayerAttribute;
import de.pixelrpg.rpg.player.PlayerClass;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.quest.Quest;
import de.pixelrpg.rpg.quest.QuestManager;
import de.pixelrpg.rpg.quest.QuestProgress;
import de.pixelrpg.rpg.shop.ShopManager;
import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Testobjekt für das native Paper Dialog-System, angebunden an den bestehenden
// Story-NPC. Bewusst hart codiert und NICHT generalisiert (siehe Memory-Notiz:
// Umbau auf JSON/datengetrieben folgt erst nach Abschluss der Testphase).
public final class StoryNpcDialogueTest implements Listener {

    private record CraftRecipe(String label, Material resultMaterial, ItemRarity rarity,
                                Map<Material, Integer> costs, int craftSeconds) {
    }

    private static final String DEMO_QUEST_ID = "hunt_zombies_example";
    private static final Key RENAME_SUBMIT_KEY = Key.key("pixelrpg", "companion_rename_submit");
    private static final String RENAME_INPUT_KEY = "companion_name";

    private static final List<CraftRecipe> RECIPES = List.of(
            new CraftRecipe("Wachenklinge", Material.IRON_SWORD, ItemRarity.RARE,
                    Map.of(Material.IRON_INGOT, 2, Material.STICK, 1), 3),
            new CraftRecipe("Wachenschild", Material.SHIELD, ItemRarity.RARE,
                    Map.of(Material.IRON_INGOT, 3, Material.OAK_PLANKS, 1), 4)
    );

    private final QuestManager questManager;
    private final ShopManager shopManager;
    private final PlayerProfileManager profileManager;
    private final NpcManager npcManager;

    // Reine Laufzeit-Nachverfolgung der Testgefährten (nicht persistent, siehe Memory-Notiz).
    private final Map<UUID, List<UUID>> companionsByPlayer = new ConcurrentHashMap<>();

    public StoryNpcDialogueTest(QuestManager questManager, ShopManager shopManager,
                                 PlayerProfileManager profileManager, NpcManager npcManager) {
        this.questManager = questManager;
        this.shopManager = shopManager;
        this.profileManager = profileManager;
        this.npcManager = npcManager;
    }

    public void begin(Player player, RPGNpc npc) {
        player.showDialog(hub(player, npc));
    }

    // --- Hub: immer dieselben Buttons in derselben Reihenfolge ---

    private Dialog hub(Player player, RPGNpc npc) {
        Component body = Component.text("Was möchtest du?", NamedTextColor.GRAY);
        List<ActionButton> buttons = List.of(
                button("Auftrag: Zombies", NamedTextColor.GREEN, () -> player.showDialog(questEntry(player, npc))),
                button("Andere Aufträge", NamedTextColor.GREEN, () -> player.showDialog(questBoardDialog(player, npc))),
                button("Handwerk", NamedTextColor.GOLD, () -> player.showDialog(craftingDialog(player, npc))),
                button("Gefährten", NamedTextColor.LIGHT_PURPLE, () -> player.showDialog(companionDialog(player, npc))),
                button("Attribute verteilen", NamedTextColor.AQUA, () -> player.showDialog(attributeDialog(player, npc))),
                button("Klasse", NamedTextColor.AQUA, () -> player.showDialog(classDialog(player, npc))),
                button("Charakter", NamedTextColor.WHITE, () -> player.showDialog(characterDialog(player, npc))),
                button("Reiseziele", NamedTextColor.LIGHT_PURPLE, () -> player.showDialog(travelDialog(player, npc))),
                button("Händler", NamedTextColor.AQUA, () -> openShop(player, npc)),
                button("Über diesen Ort", NamedTextColor.YELLOW, () -> player.showDialog(loreDialog(player, npc))),
                button("Auf Wiedersehen", NamedTextColor.DARK_GRAY, player::closeDialog)
        );
        return buildMultiAction(npc.name(), List.of(DialogBody.plainMessage(body)), buttons);
    }

    // --- Zombie-Testquest ---

    private Dialog questEntry(Player player, RPGNpc npc) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        Quest quest = questManager.getRepository().getQuest(DEMO_QUEST_ID);
        if (profile == null || quest == null) {
            return hub(player, npc);
        }
        if (profile.hasCompletedQuest(DEMO_QUEST_ID)) {
            return questCompletedDialog(player, npc);
        }
        if (profile.hasActiveQuest(DEMO_QUEST_ID)) {
            QuestProgress progress = profile.getActiveQuests().get(DEMO_QUEST_ID);
            return questActiveDialog(player, npc, quest, progress);
        }
        return questIntroDialog(player, npc, quest);
    }

    private Dialog questIntroDialog(Player player, RPGNpc npc, Quest quest) {
        Component body = Component.text(
                "Seit einigen Nächten werden die Felder von Zombies heimgesucht. "
                        + "Kannst du dich um das Problem kümmern?", NamedTextColor.GRAY);
        List<ActionButton> buttons = List.of(
                button("Ja, ich kümmere mich darum.", NamedTextColor.GREEN, () -> {
                    boolean accepted = questManager.acceptQuest(player, quest);
                    player.showDialog(accepted ? questAcceptedDialog(player, npc) : questFailedDialog(player, npc));
                }),
                button("Nein, tut mir leid.", NamedTextColor.RED, () -> player.showDialog(questDeclinedDialog(player, npc))),
                backButton(player, npc)
        );
        return buildMultiAction(npc.name(), List.of(DialogBody.plainMessage(body)), buttons);
    }

    private Dialog questAcceptedDialog(Player player, RPGNpc npc) {
        Component body = Component.text("Ich danke dir! Bring die Ruhe zurück in unsere Felder.", NamedTextColor.GRAY);
        return buildMultiAction(npc.name(), List.of(DialogBody.plainMessage(body)), List.of(backButton(player, npc)));
    }

    private Dialog questFailedDialog(Player player, RPGNpc npc) {
        Component body = Component.text(
                "Das klappt gerade nicht - schau in deinem Questlog nach, ob dort noch Platz ist.", NamedTextColor.RED);
        return buildMultiAction(npc.name(), List.of(DialogBody.plainMessage(body)), List.of(backButton(player, npc)));
    }

    private Dialog questDeclinedDialog(Player player, RPGNpc npc) {
        Component body = Component.text("Wie du meinst. Vielleicht überlegst du es dir noch einmal.", NamedTextColor.GRAY);
        return buildMultiAction(npc.name(), List.of(DialogBody.plainMessage(body)), List.of(backButton(player, npc)));
    }

    private Dialog questActiveDialog(Player player, RPGNpc npc, Quest quest, QuestProgress progress) {
        boolean completable = progress.getCurrentAmount() >= quest.requiredAmount();
        Component body = Component.text("Wie sieht es mit den Zombies aus? Du hast bisher "
                + progress.getCurrentAmount() + "/" + quest.requiredAmount() + " erledigt.", NamedTextColor.GRAY);

        List<ActionButton> buttons = new ArrayList<>();
        if (completable) {
            buttons.add(button("Quest abschließen", NamedTextColor.GREEN, () -> {
                questManager.completeQuest(player, DEMO_QUEST_ID);
                player.showDialog(questCompletedDialog(player, npc));
            }));
        }
        buttons.add(backButton(player, npc));
        return buildMultiAction(npc.name(), List.of(DialogBody.plainMessage(body)), buttons);
    }

    private Dialog questCompletedDialog(Player player, RPGNpc npc) {
        Component body = Component.text("Danke, dass du die Felder von den Zombies befreit hast!", NamedTextColor.GRAY);
        return buildMultiAction(npc.name(), List.of(DialogBody.plainMessage(body)), List.of(backButton(player, npc)));
    }

    // --- Auftragsliste aus dem bestehenden Questsystem ---

    private Dialog questBoardDialog(Player player, RPGNpc npc) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) {
            return hub(player, npc);
        }

        List<Quest> quests = questManager.getRepository().getQuestsByCategory(profile.getRank()).stream()
                .filter(quest -> !profile.hasCompletedQuest(quest.id()))
                .filter(quest -> !profile.hasActiveQuest(quest.id()))
                .limit(4)
                .toList();

        List<DialogBody> body = new ArrayList<>();
        if (quests.isEmpty()) {
            body.add(DialogBody.plainMessage(Component.text("Gerade nichts Neues für deinen Rang verfügbar.", NamedTextColor.GRAY)));
        } else {
            for (Quest quest : quests) {
                body.add(DialogBody.plainMessage(Component.text(quest.title() + " – " + quest.description(), NamedTextColor.GRAY)));
            }
        }

        List<ActionButton> buttons = new ArrayList<>();
        for (Quest quest : quests) {
            buttons.add(button("Annehmen: " + quest.title(), NamedTextColor.GREEN, () -> {
                boolean accepted = questManager.acceptQuest(player, quest);
                player.showDialog(accepted ? hub(player, npc) : questFailedDialog(player, npc));
            }));
        }
        buttons.add(backButton(player, npc));
        return buildMultiAction(npc.name(), body, buttons);
    }

    // --- Handwerk: ein Button pro Rezept (Item + Materialien + Dauer in einer Zeile), Klick stellt direkt her ---

    private Dialog craftingDialog(Player player, RPGNpc npc) {
        List<DialogBody> body = List.of(DialogBody.plainMessage(
                Component.text("Bring mir die Materialien, ich fertige dir Ausrüstung deines Rangs.", NamedTextColor.GRAY)));

        List<ActionButton> buttons = new ArrayList<>();
        for (CraftRecipe recipe : RECIPES) {
            Component label = Component.text(
                    recipe.label() + " – " + describeCosts(recipe) + " – " + recipe.craftSeconds() + "s",
                    NamedTextColor.GOLD);
            buttons.add(button(label, () -> tryCraft(player, npc, recipe)));
        }
        buttons.add(backButton(player, npc));
        return buildMultiAction(npc.name(), body, buttons);
    }

    private String describeCosts(CraftRecipe recipe) {
        StringBuilder builder = new StringBuilder();
        for (var entry : recipe.costs().entrySet()) {
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append(entry.getValue()).append("x ").append(prettyMaterial(entry.getKey()));
        }
        return builder.toString();
    }

    private String prettyMaterial(Material material) {
        String raw = material.name().replace('_', ' ').toLowerCase();
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }

    private void tryCraft(Player player, RPGNpc npc, CraftRecipe recipe) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) {
            return;
        }

        Material missing = null;
        for (var entry : recipe.costs().entrySet()) {
            if (!player.getInventory().containsAtLeast(new ItemStack(entry.getKey()), entry.getValue())) {
                missing = entry.getKey();
                break;
            }
        }
        if (missing != null) {
            player.showDialog(craftMissingMaterialsDialog(player, npc, recipe, missing));
            return;
        }

        for (var entry : recipe.costs().entrySet()) {
            player.getInventory().removeItem(new ItemStack(entry.getKey(), entry.getValue()));
        }

        player.closeDialog();
        player.sendMessage(Component.text(recipe.label() + " wird gefertigt ...", NamedTextColor.YELLOW));
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);

        Bukkit.getScheduler().runTaskLater(PixelRPGPlugin.getInstance(), () -> {
            if (!player.isOnline()) {
                return;
            }
            RPGItemBuilder.createUnidentified(recipe.resultMaterial(), recipe.rarity(), profile.getRank())
                    .map(RPGItemBuilder::identify)
                    .ifPresent(item -> player.getInventory().addItem(item).values()
                            .forEach(remainder -> player.getWorld().dropItemNaturally(player.getLocation(), remainder)));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            player.sendMessage(Component.text(recipe.label() + " ist fertig!", NamedTextColor.GREEN));
        }, recipe.craftSeconds() * 20L);
    }

    private Dialog craftMissingMaterialsDialog(Player player, RPGNpc npc, CraftRecipe recipe, Material missing) {
        Component body = Component.text(
                "Dir fehlt noch: " + prettyMaterial(missing) + " (für " + recipe.label() + ")", NamedTextColor.RED);
        return buildMultiAction(npc.name(), List.of(DialogBody.plainMessage(body)),
                List.of(button("Zurück", NamedTextColor.DARK_GRAY, () -> player.showDialog(craftingDialog(player, npc)))));
    }

    // --- Gefährten: Demo-Beschwörung, umbenennbar, wegschickbar, nicht persistent ---

    private Dialog companionDialog(Player player, RPGNpc npc) {
        List<UUID> tracked = companionsByPlayer.getOrDefault(player.getUniqueId(), List.of());

        long alive = 0;
        for (UUID entityId : tracked) {
            Entity entity = Bukkit.getEntity(entityId);
            if (entity != null && entity.isValid()) {
                alive++;
            }
        }

        Component body = Component.text(
                "Diese Gefährten würden sich dir anschließen (nur Demo, kein dauerhaftes System). "
                        + "Aktuell begleiten dich: " + alive, NamedTextColor.GRAY);

        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(button("Wolf beschwören", NamedTextColor.LIGHT_PURPLE, () -> summonCompanion(player, npc, EntityType.WOLF, "Wolf")));
        buttons.add(button("Katze beschwören", NamedTextColor.LIGHT_PURPLE, () -> summonCompanion(player, npc, EntityType.CAT, "Katze")));
        if (alive > 0) {
            buttons.add(button("Gefährten umbenennen", NamedTextColor.YELLOW, () -> player.showDialog(companionRenameDialog(player, npc))));
            buttons.add(button("Gefährten wegschicken", NamedTextColor.RED, () -> player.showDialog(companionDismissConfirmDialog(player, npc))));
        }
        buttons.add(backButton(player, npc));
        return buildMultiAction(npc.name(), List.of(DialogBody.plainMessage(body)), buttons);
    }

    private void summonCompanion(Player player, RPGNpc npc, EntityType type, String label) {
        Location location = player.getLocation();
        Entity entity = location.getWorld().spawnEntity(location, type);
        if (entity instanceof Tameable tameable) {
            tameable.setTamed(true);
            tameable.setOwner(player);
        }
        entity.customName(Component.text(label, NamedTextColor.LIGHT_PURPLE));
        entity.setCustomNameVisible(true);
        companionsByPlayer.computeIfAbsent(player.getUniqueId(), key -> new ArrayList<>()).add(entity.getUniqueId());
        player.sendMessage(Component.text(label + " begleitet dich jetzt (nur Demo, nicht gespeichert).", NamedTextColor.LIGHT_PURPLE));
        player.showDialog(companionDialog(player, npc));
    }

    private Dialog companionDismissConfirmDialog(Player player, RPGNpc npc) {
        Component body = Component.text("Wirklich alle Gefährten wegschicken?", NamedTextColor.GRAY);
        ActionButton yes = button("Ja, wegschicken", NamedTextColor.RED, () -> {
            dismissCompanions(player);
            player.showDialog(companionDialog(player, npc));
        });
        ActionButton no = button("Nein, doch nicht", NamedTextColor.GRAY, () -> player.showDialog(companionDialog(player, npc)));
        return build(npc.name(), List.of(DialogBody.plainMessage(body)), DialogType.confirmation(yes, no));
    }

    private void dismissCompanions(Player player) {
        List<UUID> tracked = companionsByPlayer.remove(player.getUniqueId());
        if (tracked == null) {
            return;
        }
        for (UUID entityId : tracked) {
            Entity entity = Bukkit.getEntity(entityId);
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }
    }

    // --- Gefährten umbenennen: Text-Eingabefeld, Auswertung über Key-basierten CustomClick ---

    private Dialog companionRenameDialog(Player player, RPGNpc npc) {
        DialogInput nameInput = DialogInput.text(RENAME_INPUT_KEY, Component.text("Name")).build();

        DialogBase base = DialogBase.builder(Component.text(npc.name(), NamedTextColor.GOLD))
                .body(List.of(DialogBody.plainMessage(Component.text("Wie soll dein Gefährte heißen?", NamedTextColor.GRAY))))
                .inputs(List.of(nameInput))
                .canCloseWithEscape(true)
                .build();

        ActionButton submit = ActionButton.builder(Component.text("Bestätigen", NamedTextColor.GREEN))
                .action(DialogAction.customClick(RENAME_SUBMIT_KEY, null))
                .build();
        ActionButton cancel = button("Abbrechen", NamedTextColor.DARK_GRAY, () -> player.showDialog(companionDialog(player, npc)));

        return Dialog.create(factory -> factory.empty().base(base).type(DialogType.multiAction(List.of(submit, cancel)).build()));
    }

    @EventHandler
    public void onCustomClick(PlayerCustomClickEvent event) {
        if (!RENAME_SUBMIT_KEY.equals(event.getIdentifier())) {
            return;
        }
        if (!(event.getCommonConnection() instanceof PlayerGameConnection gameConnection)) {
            return;
        }
        Player player = gameConnection.getPlayer();

        DialogResponseView response = event.getDialogResponseView();
        if (response == null) {
            return;
        }
        String newName = response.getText(RENAME_INPUT_KEY);
        if (newName == null || newName.isBlank()) {
            return;
        }

        renameLatestCompanion(player, newName);
        player.sendMessage(Component.text("Umbenannt zu: " + newName, NamedTextColor.LIGHT_PURPLE));
        player.closeDialog();
    }

    private void renameLatestCompanion(Player player, String newName) {
        List<UUID> tracked = companionsByPlayer.getOrDefault(player.getUniqueId(), List.of());
        for (int i = tracked.size() - 1; i >= 0; i--) {
            Entity entity = Bukkit.getEntity(tracked.get(i));
            if (entity != null && entity.isValid()) {
                entity.customName(Component.text(newName, NamedTextColor.LIGHT_PURPLE));
                return;
            }
        }
    }

    // --- Charakter-Info ---

    private Dialog characterDialog(Player player, RPGNpc npc) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) {
            return hub(player, npc);
        }

        long hours = profile.getPlaytimeMillis() / 3_600_000L;
        long minutes = (profile.getPlaytimeMillis() / 60_000L) % 60L;

        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text("Rang: ", NamedTextColor.GRAY).append(profile.getRank().displayName())),
                DialogBody.plainMessage(Component.text("Klasse: ", NamedTextColor.GRAY).append(profile.getPlayerClass().displayName())),
                DialogBody.plainMessage(Component.text("Erfahrung: " + profile.getExperience(), NamedTextColor.GRAY)),
                DialogBody.plainMessage(Component.text("Gold: " + String.format("%.0f", profile.getMoney()), NamedTextColor.GOLD)),
                DialogBody.plainMessage(Component.text("Spielzeit: " + hours + "h " + minutes + "min", NamedTextColor.GRAY)),
                DialogBody.plainMessage(Component.text("Monster besiegt: " + profile.getStatistic("MOBS_KILLED"), NamedTextColor.GRAY))
        );

        return build(npc.name(), body, DialogType.notice(backButton(player, npc)));
    }

    // --- Attributverteilung ---

    private Dialog attributeDialog(Player player, RPGNpc npc) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) {
            return hub(player, npc);
        }

        List<DialogBody> body = List.of(DialogBody.plainMessage(
                Component.text("Verfügbares Gold: " + String.format("%.0f", profile.getMoney()), NamedTextColor.GOLD)));

        List<ActionButton> buttons = new ArrayList<>();
        for (PlayerAttribute attribute : PlayerAttribute.values()) {
            int current = profile.getAttributePoints(attribute);
            int max = attribute.getMaxPoints();
            if (current >= max) {
                continue;
            }
            double cost = AttributeConfig.costFor(attribute, current, profile.getPlayerClass());
            Component label = attribute.displayName()
                    .append(Component.text(" (" + current + "/" + max + ") – " + (long) cost + " Gold", NamedTextColor.GRAY));
            buttons.add(button(label, () -> {
                PlayerProfileManager.AttributePurchaseResult result = profileManager.purchaseAttribute(player, attribute);
                if (result == PlayerProfileManager.AttributePurchaseResult.SUCCESS) {
                    PixelRPGPlugin.getInstance().getStatEngine().recalculate(player);
                }
                player.showDialog(attributeResultDialog(player, npc, result));
            }));
        }
        buttons.add(backButton(player, npc));
        return buildMultiAction(npc.name(), body, buttons);
    }

    private Dialog attributeResultDialog(Player player, RPGNpc npc, PlayerProfileManager.AttributePurchaseResult result) {
        String text = switch (result) {
            case SUCCESS -> "Attribut erhöht!";
            case MAX_REACHED -> "Dieses Attribut ist bereits maximiert.";
            case RANK_TOO_LOW -> "Dein Rang ist dafür noch zu niedrig.";
            case INSUFFICIENT_FUNDS -> "Du hast nicht genug Gold.";
            case NOT_REGISTERED -> "Du musst registriertes Rathausmitglied sein.";
        };
        NamedTextColor color = result == PlayerProfileManager.AttributePurchaseResult.SUCCESS ? NamedTextColor.GREEN : NamedTextColor.RED;
        Component body = Component.text(text, color);
        List<ActionButton> buttons = List.of(
                button("Weiter verteilen", NamedTextColor.AQUA, () -> player.showDialog(attributeDialog(player, npc))),
                backButton(player, npc)
        );
        return buildMultiAction(npc.name(), List.of(DialogBody.plainMessage(body)), buttons);
    }

    // --- Klasse ---

    private Dialog classDialog(Player player, RPGNpc npc) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) {
            return hub(player, npc);
        }

        Component body = profile.getPlayerClass().isNone()
                ? Component.text("Du hast noch keine Klasse gewählt. Wähle eine:", NamedTextColor.GRAY)
                : Component.text("Aktuelle Klasse: ", NamedTextColor.GRAY).append(profile.getPlayerClass().displayName());

        List<ActionButton> buttons = new ArrayList<>();
        for (PlayerClass playerClass : PlayerClass.values()) {
            if (playerClass == PlayerClass.NONE || playerClass == profile.getPlayerClass()) {
                continue;
            }
            buttons.add(button(playerClass.displayName(), () -> {
                boolean changed = profile.getPlayerClass().isNone()
                        ? profileManager.selectClass(player, playerClass)
                        : profileManager.respecClass(player, playerClass) == PlayerProfileManager.RespecResult.SUCCESS;
                player.showDialog(changed ? classDialog(player, npc) : classFailedDialog(player, npc));
            }));
        }
        buttons.add(backButton(player, npc));
        return buildMultiAction(npc.name(), List.of(DialogBody.plainMessage(body)), buttons);
    }

    private Dialog classFailedDialog(Player player, RPGNpc npc) {
        Component body = Component.text("Das hat nicht geklappt - prüfe Rang oder Gold.", NamedTextColor.RED);
        return buildMultiAction(npc.name(), List.of(DialogBody.plainMessage(body)), List.of(backButton(player, npc)));
    }

    // --- Reiseziele ---

    private Dialog travelDialog(Player player, RPGNpc npc) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) {
            return hub(player, npc);
        }

        List<RPGNpc> destinations = npcManager.getAll().stream()
                .filter(candidate -> candidate.type() == NpcType.TRAVEL)
                .filter(candidate -> profile.hasUnlockedWaypoint(candidate.id()))
                .limit(6)
                .toList();

        List<DialogBody> body = new ArrayList<>();
        if (destinations.isEmpty()) {
            body.add(DialogBody.plainMessage(Component.text("Noch keine Reiseziele entdeckt.", NamedTextColor.GRAY)));
        }

        List<ActionButton> buttons = new ArrayList<>();
        for (RPGNpc destination : destinations) {
            buttons.add(button(destination.name(), NamedTextColor.LIGHT_PURPLE, () -> {
                player.closeDialog();
                player.teleportAsync(destination.location().clone().add(0, 1, 0));
            }));
        }
        buttons.add(backButton(player, npc));
        return buildMultiAction(npc.name(), body, buttons);
    }

    // --- Nebengespräch ---

    private Dialog loreDialog(Player player, RPGNpc npc) {
        Component body = Component.text(
                "Dieses Dorf steht schon seit Generationen an dieser Stelle. "
                        + "Früher war es hier ruhig - bis vor Kurzem.", NamedTextColor.GRAY);
        return buildMultiAction(npc.name(), List.of(DialogBody.plainMessage(body)), List.of(backButton(player, npc)));
    }

    // --- Shop ---

    private void openShop(Player player, RPGNpc npc) {
        player.closeDialog();
        new ShopGUI(player, npc.id(), shopManager, profileManager).open(player);
    }

    // --- Hilfsmethoden ---

    private ActionButton backButton(Player player, RPGNpc npc) {
        return button("Zurück zur Übersicht", NamedTextColor.DARK_GRAY, () -> player.showDialog(hub(player, npc)));
    }

    private ActionButton button(String label, NamedTextColor color, Runnable action) {
        return button(Component.text(label, color), action);
    }

    private ActionButton button(Component label, Runnable action) {
        return ActionButton.builder(label)
                .action(customAction(action))
                .build();
    }

    private Dialog build(String npcName, List<DialogBody> body, DialogType type) {
        DialogBase base = DialogBase.builder(Component.text(npcName, NamedTextColor.GOLD))
                .body(body)
                .canCloseWithEscape(true)
                .build();
        return Dialog.create(factory -> factory.empty().base(base).type(type));
    }

    private Dialog buildMultiAction(String npcName, List<DialogBody> body, List<ActionButton> buttons) {
        return build(npcName, body, DialogType.multiAction(buttons).build());
    }

    private DialogAction customAction(Runnable action) {
        DialogActionCallback callback = (response, audience) -> action.run();
        return DialogAction.customClick(callback, ClickCallback.Options.builder().build());
    }
}