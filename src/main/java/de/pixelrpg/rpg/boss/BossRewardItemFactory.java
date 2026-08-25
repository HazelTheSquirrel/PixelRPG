package de.pixelrpg.rpg.boss;

import de.pixelrpg.rpg.item.ItemRarity;
import de.pixelrpg.rpg.item.RPGItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.Optional;

public final class BossRewardItemFactory {
    private BossRewardItemFactory() {
    }

    public static Optional<ItemStack> create(String id, int level) {
        if (id == null || id.isBlank()) return Optional.empty();
        Reward reward = reward(id);
        if (reward == null) return Optional.empty();
        return RPGItemBuilder.createItem("pixelrpg:boss/" + reward.id(), reward.name(), Material.BRUSH,
                        reward.rarity(), Math.max(1, Math.min(99, level)))
                .map(item -> RPGItemBuilder.withWeaponAbility(item, reward.ability(), reward.cooldownMillis()));
    }

    private static Reward reward(String raw) {
        String id = raw.toLowerCase(Locale.ROOT).replace("pixelrpg:boss/", "");
        return switch (id) {
            case "pluenderer_siegel" -> new Reward(id, "Plünderer-Siegel", ItemRarity.UNCOMMON, "PLUNDERER_SEAL", 10_000L);
            case "bienenkoenigin" -> new Reward(id, "Bienenkönigin-Siegel", ItemRarity.RARE, "BEE_QUEEN_MARK", 10_000L);
            case "hexenkessel" -> new Reward(id, "Hexenkessel", ItemRarity.RARE, "WITCH_CAULDRON", 15_000L);
            case "knarzendes_herzstueck" -> new Reward(id, "Knarzendes Herzstück", ItemRarity.EPIC, "KNAARZ_HEART", 20_000L);
            case "dschungel_amulett" -> new Reward(id, "Dschungel-Amulett", ItemRarity.RARE, "JUNGLE_AMULET", 12_000L);
            case "sumpftrank" -> new Reward(id, "Sumpftrank", ItemRarity.RARE, "SWAMP_POTION", 12_000L);
            case "husk_siegel" -> new Reward(id, "Husk-Siegel", ItemRarity.RARE, "HUSK_SEAL", 12_000L);
            case "ravager_trophaee" -> new Reward(id, "Ravager-Trophäe", ItemRarity.EPIC, "RAVAGER_TROPHY", 18_000L);
            case "goldenes_fossil" -> new Reward(id, "Goldenes Fossil", ItemRarity.RARE, "GOLDEN_FOSSIL", 20_000L);
            case "frostwolf_fang" -> new Reward(id, "Frostwolf-Fang", ItemRarity.EPIC, "FROSTWOLF_FANG", 15_000L);
            case "frostpfeil_koecher" -> new Reward(id, "Frostpfeil-Köcher", ItemRarity.EPIC, "FROST_ARROW_QUIVER", 15_000L);
            case "horn_des_berges" -> new Reward(id, "Horn des Berges", ItemRarity.EPIC, "MOUNTAIN_HORN", 20_000L);
            case "wildhorn" -> new Reward(id, "Wildhorn", ItemRarity.RARE, "WILD_HORN", 15_000L);
            case "bluetenhonig" -> new Reward(id, "Blütenhonig", ItemRarity.RARE, "BLOSSOM_HONEY", 10_000L);
            case "kapitaens_nautilus" -> new Reward(id, "Kapitäns-Nautilus", ItemRarity.EPIC, "CAPTAINS_NAUTILUS", 15_000L);
            case "flusskiesel" -> new Reward(id, "Flusskiesel", ItemRarity.RARE, "RIVER_PEBBLE", 12_000L);
            case "auge_der_tiefe" -> new Reward(id, "Auge der Tiefe", ItemRarity.EPIC, "EYE_OF_DEPTH", 20_000L);
            case "myzelkern" -> new Reward(id, "Myzelkern", ItemRarity.EPIC, "MYCELIUM_CORE", 15_000L);
            case "spinnenauge_des_jaegers" -> new Reward(id, "Spinnenauge des Jägers", ItemRarity.RARE, "SPIDER_EYE_HUNTER", 12_000L);
            case "echoherz" -> new Reward(id, "Echoherz", ItemRarity.LEGENDARY, "ECHO_HEART", 25_000L);
            case "netherkern" -> new Reward(id, "Netherkern", ItemRarity.EPIC, "NETHER_CORE", 20_000L);
            case "karmesinherz" -> new Reward(id, "Karmesinherz", ItemRarity.EPIC, "CRIMSON_HEART", 20_000L);
            case "gebundene_enderperle" -> new Reward(id, "Gebundene Enderperle", ItemRarity.LEGENDARY, "BOUND_ENDER_PEARL", 30_000L);
            case "seelenfragment" -> new Reward(id, "Seelenfragment", ItemRarity.EPIC, "SOUL_FRAGMENT", 20_000L);
            case "magmaherz" -> new Reward(id, "Magmaherz", ItemRarity.EPIC, "MAGMA_HEART", 20_000L);
            case "shulkerkern" -> new Reward(id, "Shulkerkern", ItemRarity.LEGENDARY, "SHULKER_CORE", 25_000L);
            case "risskern" -> new Reward(id, "Risskern", ItemRarity.LEGENDARY, "RIFT_CORE", 30_000L);
            case "sturmherz" -> new Reward(id, "Sturmherz", ItemRarity.LEGENDARY, "STORM_HEART", 30_000L);
            case "abgrundkern" -> new Reward(id, "Abgrundkern", ItemRarity.LEGENDARY, "ABYSS_CORE", 30_000L);
            case "seelenkrone" -> new Reward(id, "Seelenkrone", ItemRarity.LEGENDARY, "SOUL_CROWN", 30_000L);
            case "endriss" -> new Reward(id, "Endriss", ItemRarity.LEGENDARY, "END_RIFT", 30_000L);
            case "weltenherz" -> new Reward(id, "Weltenherz", ItemRarity.LEGENDARY, "WORLD_HEART", 35_000L);
            default -> null;
        };
    }

    private record Reward(String id, String name, ItemRarity rarity, String ability, long cooldownMillis) {
    }
}
