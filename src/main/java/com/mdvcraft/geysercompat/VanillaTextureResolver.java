package com.mdvcraft.geysercompat;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Convierte IDs Java a rutas de texturas vanilla de Bedrock.
 * Los bloques normalmente usan el render 3D y no pasan por este resolver.
 */
final class VanillaTextureResolver {
    private static final Map<String, String> EXACT = new HashMap<>();
    private static final Set<String> HANDHELD_EXACT = Set.of(
            "bow", "crossbow", "trident", "mace", "fishing_rod", "carrot_on_a_stick",
            "warped_fungus_on_a_stick", "stick", "blaze_rod", "breeze_rod", "brush"
    );

    static {
        // Comida / materiales con nombres diferentes entre Java y Bedrock.
        alias("beef", "textures/items/beef_raw");
        alias("cooked_beef", "textures/items/beef_cooked");
        alias("chicken", "textures/items/chicken_raw");
        alias("cooked_chicken", "textures/items/chicken_cooked");
        alias("mutton", "textures/items/mutton_raw");
        alias("cooked_mutton", "textures/items/mutton_cooked");
        alias("porkchop", "textures/items/porkchop_raw");
        alias("cooked_porkchop", "textures/items/porkchop_cooked");
        alias("rabbit", "textures/items/rabbit_raw");
        alias("cooked_rabbit", "textures/items/rabbit_cooked");
        alias("cod", "textures/items/fish_raw");
        alias("cooked_cod", "textures/items/fish_cooked");
        alias("salmon", "textures/items/fish_salmon");
        alias("cooked_salmon", "textures/items/fish_salmon_cooked");
        alias("tropical_fish", "textures/items/fish_clownfish_raw");
        alias("pufferfish", "textures/items/fish_pufferfish_raw");
        alias("golden_carrot", "textures/items/carrot_golden");
        alias("melon_slice", "textures/items/melon");
        alias("glistering_melon_slice", "textures/items/melon_speckled");
        alias("popped_chorus_fruit", "textures/items/chorus_fruit_popped");
        alias("nether_brick", "textures/items/netherbrick");
        alias("dragon_breath", "textures/items/dragons_breath");
        alias("nautilus_shell", "textures/items/nautilus");
        alias("heart_of_the_sea", "textures/items/heartofthesea_closed");

        // UI / estados de items.
        alias("bow", "textures/items/bow_standby");
        alias("crossbow", "textures/items/crossbow_standby");
        alias("fishing_rod", "textures/items/fishing_rod_uncast");
        alias("clock", "textures/items/clock_item");
        alias("compass", "textures/items/compass_item");
        alias("recovery_compass", "textures/items/recovery_compass_item");
        alias("map", "textures/items/map_empty");
        alias("filled_map", "textures/items/map_filled");
        alias("firework_rocket", "textures/items/fireworks");
        alias("firework_star", "textures/items/fireworks_charge");
        alias("experience_bottle", "textures/items/experience_bottle");
        alias("totem_of_undying", "textures/items/totem");
        alias("enchanted_golden_apple", "textures/items/apple_golden");
        alias("golden_apple", "textures/items/apple_golden");

        // Minecarts.
        alias("minecart", "textures/items/minecart_normal");
        alias("chest_minecart", "textures/items/minecart_chest");
        alias("furnace_minecart", "textures/items/minecart_furnace");
        alias("tnt_minecart", "textures/items/minecart_tnt");
        alias("hopper_minecart", "textures/items/minecart_hopper");
        alias("command_block_minecart", "textures/items/minecart_command_block");

        // Music discs (nombres históricos de Bedrock).
        alias("music_disc_13", "textures/items/record_13");
        alias("music_disc_cat", "textures/items/record_cat");
        alias("music_disc_blocks", "textures/items/record_blocks");
        alias("music_disc_chirp", "textures/items/record_chirp");
        alias("music_disc_far", "textures/items/record_far");
        alias("music_disc_mall", "textures/items/record_mall");
        alias("music_disc_mellohi", "textures/items/record_mellohi");
        alias("music_disc_stal", "textures/items/record_stal");
        alias("music_disc_strad", "textures/items/record_strad");
        alias("music_disc_ward", "textures/items/record_ward");
        alias("music_disc_11", "textures/items/record_11");
        alias("music_disc_wait", "textures/items/record_wait");
        alias("music_disc_pigstep", "textures/items/record_pigstep");
        alias("music_disc_otherside", "textures/items/record_otherside");
        alias("music_disc_5", "textures/items/record_5");
        alias("music_disc_relic", "textures/items/record_relic");
        alias("music_disc_creator", "textures/items/record_creator");
        alias("music_disc_creator_music_box", "textures/items/record_creator_music_box");
        alias("music_disc_precipice", "textures/items/record_precipice");
        alias("music_disc_tears", "textures/items/record_tears");
        alias("music_disc_lava_chicken", "textures/items/record_lava_chicken");
    }

    private VanillaTextureResolver() {}

    static String resolve(String javaId, Map<String, String> overrides) {
        String normalized = CompatConfig.normalizeId(javaId);
        String manual = overrides.get(normalized);
        if (manual != null && !manual.isBlank()) return manual;

        String path = normalized.substring(normalized.indexOf(':') + 1);
        String exact = EXACT.get(path);
        if (exact != null) return exact;

        // Herramientas: Bedrock usa wood_* y gold_* en lugar de wooden_*/golden_*.
        if (path.startsWith("wooden_") && isToolSuffix(path)) {
            return "textures/items/wood_" + path.substring("wooden_".length());
        }
        if (path.startsWith("golden_") && isToolSuffix(path)) {
            return "textures/items/gold_" + path.substring("golden_".length());
        }

        // Armaduras doradas también usan gold_*.
        if (path.startsWith("golden_") && isArmorSuffix(path)) {
            return "textures/items/gold_" + path.substring("golden_".length());
        }

        // Buckets vanilla.
        if (path.equals("bucket")) return "textures/items/bucket_empty";
        if (path.equals("water_bucket")) return "textures/items/bucket_water";
        if (path.equals("lava_bucket")) return "textures/items/bucket_lava";
        if (path.equals("milk_bucket")) return "textures/items/bucket_milk";
        if (path.endsWith("_bucket")) {
            String mob = path.substring(0, path.length() - "_bucket".length());
            return switch (mob) {
                case "cod" -> "textures/items/bucket_cod";
                case "salmon" -> "textures/items/bucket_salmon";
                case "tropical_fish" -> "textures/items/bucket_tropical";
                case "pufferfish" -> "textures/items/bucket_pufferfish";
                case "axolotl" -> "textures/items/bucket_axolotl";
                case "tadpole" -> "textures/items/bucket_tadpole";
                case "powder_snow" -> "textures/items/bucket_powder_snow";
                default -> "textures/items/bucket_" + mob;
            };
        }

        // Spawn eggs: Bedrock mantiene las texturas bajo este subdirectorio.
        if (path.endsWith("_spawn_egg")) {
            String mob = path.substring(0, path.length() - "_spawn_egg".length());
            return "textures/items/spawn_eggs/spawn_egg_" + mob;
        }

        // La mayoría de items modernos coincide 1:1.
        return "textures/items/" + path;
    }

    static String resolveFlatBlock(String javaId, Map<String, String> overrides) {
        String normalized = CompatConfig.normalizeId(javaId);
        String manual = overrides.get(normalized);
        if (manual != null && !manual.isBlank()) return manual;
        String path = normalized.substring(normalized.indexOf(':') + 1);
        return "textures/blocks/" + path;
    }

    static boolean displayHandheld(String javaId) {
        String path = CompatConfig.normalizeId(javaId);
        path = path.substring(path.indexOf(':') + 1).toLowerCase(Locale.ROOT);
        return HANDHELD_EXACT.contains(path) || isToolSuffix(path);
    }

    static String iconKey(String baseId, String targetId) {
        String base = sanitize(CompatConfig.normalizeId(baseId));
        String target = sanitize(CompatConfig.normalizeId(targetId));
        return "mdvcompat." + base + "__as__" + target;
    }

    static String bedrockIdentifier(String baseId, String targetId) {
        String base = sanitize(CompatConfig.normalizeId(baseId));
        String target = sanitize(CompatConfig.normalizeId(targetId));
        String path = base + "__as__" + target;
        if (path.length() > 110) path = Integer.toHexString(path.hashCode()) + "_" + target;
        return "mdvcompat:" + path;
    }

    private static boolean isToolSuffix(String path) {
        return path.endsWith("_sword") || path.endsWith("_pickaxe") || path.endsWith("_axe")
                || path.endsWith("_shovel") || path.endsWith("_hoe");
    }

    private static boolean isArmorSuffix(String path) {
        return path.endsWith("_helmet") || path.endsWith("_chestplate")
                || path.endsWith("_leggings") || path.endsWith("_boots");
    }

    private static String sanitize(String id) {
        return id.toLowerCase(Locale.ROOT)
                .replace("minecraft:", "")
                .replace(':', '_')
                .replace('/', '_')
                .replaceAll("[^a-z0-9._-]", "_");
    }

    private static void alias(String id, String texture) {
        EXACT.put(id, texture);
    }
}
