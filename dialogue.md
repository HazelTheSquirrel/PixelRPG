# Dialogue – Forensische Analyse

Geprüft wurde ausschließlich Branch `test`.

## Ergebnis

Die aktuelle Dialogarchitektur besteht aus zwei getrennten Ebenen: `DialogueEngine` als technische Factory für native Paper-26.2-Dialoge und `DialogueTree`/`DialogueNode`/`DialogueOption`/`DialogueCondition`/`DialogueTreeService` als vorhandene Infrastruktur für persistente, verzweigte Dialoge.

Der entscheidende Befund: `DialogueTreeService` wird im aktuellen Runtime-Setup nicht erzeugt, kein `DialogueTree` wird registriert und kein NPC verwendet den Tree-Pfad.

Die reale NPC-Kette ist:

`PlayerInteractEntityEvent -> NpcInteractListener -> NpcManager -> NpcBehaviorRegistry -> NpcBehavior -> DialogueEngine/Feature-Dialog`.

Story-NPCs laufen derzeit über `StoryBehavior -> StoryManager -> StoryChapter -> DialogueEngine -> StoryBookFactory` und besitzen keinen echten verzweigten NPC-Dialog.

## Aktuelle Verknüpfungen

- RECEPTION -> ReceptionBehavior -> ReceptionDialog
- QUEST -> QuestBehavior -> DialogueEngine -> QuestRepository/QuestManager
- FILLER -> FillerBehavior -> DialogueEngine
- PROFESSION_* -> ProfessionTrainerBehavior -> ProfessionService/ProfessionDialog
- SHOP -> ShopBehavior -> DialogueEngine -> ShopGUI
- TRAVEL -> TravelBehavior -> TravelDialog/Waypoint-System
- BANKER -> BankerBehavior -> GuildBankAccessDialog -> BankDialog
- STORY -> StoryBehavior -> StoryManager -> StoryBookFactory

Die Charakterkarte/G-Interaktion ist ein separater Native-Dialog-Pfad über `PixelRPGBootstrap`, `DialogKeys`, `QUICK_ACTIONS` und `QuickActionsDialogListener`.

## Auffälligkeiten

1. `DialogueTreeService` ist derzeit ungenutzte Infrastruktur.
2. `/pixelrpgdialogue` ist nur ein Placeholder und startet keinen echten Tree.
3. `StoryNpcDialogue` und `DialogueTreeService` verwenden beide `DialogueProgressStore`, während der eigentliche Storyfortschritt zusätzlich über `PlayerProfile.storyChapterIndex` läuft.
4. `FillerBehavior` ruft `QuestManager.progressTalkToNpc` erneut auf, obwohl `NpcInteractListener` dies bereits vor dem Behavior tut.
5. `DialogueEngine` ist sinnvoll zentralisiert und sollte nicht mit fachlicher Storylogik belastet werden.

## Empfohlene Architektur

Nicht alle bestehenden Feature-Dialoge in einen einzigen universellen Baum pressen.

Drei Schichten:

1. `DialogueEngine` – native Dialog-Erzeugung.
2. `DialogueTreeService` – Ausführung echter narrativer/verzweigter NPC-Dialoge.
3. Feature-Dialoge – Quest, Beruf, Bank, Reise, Rezeption, Shop, Begleiter, Gilde.

Ziel:

`NPC -> NpcBehavior -> DialogueTreeService -> DialogueTree -> DialogueNode -> DialogueOption -> Condition/Action/nextNode`

Feature-NPCs dürfen weiterhin direkt ihre spezialisierten Dialogmodule verwenden.

## Story-NPC

Der Story-NPC sollte von einem einfachen Kapitel-Button auf einen echten Tree umgestellt werden.

Beispiel:

`introduction -> Wer bist du? -> lore_01`

`introduction -> Was ist hier passiert? -> lore_02`

`introduction -> Ich will helfen. -> quest_offer`

Der Tree sollte vorhandene Services wie `QuestManager`, `StoryManager`, `ProfessionService` und `PlayerProfileManager` über klar definierte Conditions und Actions ansprechen.

## Fortschritt

`PlayerProfile.storyChapterIndex` sollte den globalen linearen Storyfortschritt behalten.

`DialogueProgressStore` sollte ausschließlich Dialogzustände verwalten: gesehen/abgeschlossen.

## Prioritäten

1. `DialogueTreeService` in den Plugin-Lifecycle integrieren.
2. Zentralen Dialog-Registry-/Bootstrap-Punkt schaffen.
3. Story-NPCs an `DialogueTreeService` anbinden.
4. `StoryNpcDialogue` auf den Tree-Pfad umstellen oder entfernen.
5. Doppeltes `progressTalkToNpc` beim FILLER entfernen.
6. Wiederverwendbare Conditions und fachliche Actions schaffen.
7. Erst danach Story-Content schrittweise aus Java herauslösen.

## Was erhalten bleiben sollte

`DialogueEngine`, native Paper-26.2-Dialoge, Quest-/Berufs-/Bank-/Reise-/Shop-Dialoge, Charakterkarte/G-Interaktion und bestehende Feature-GUIs sollten zunächst erhalten bleiben.

Die Tree-Schicht sollte zuerst ergänzen und gezielt den narrativen Story-/NPC-Pfad übernehmen.
