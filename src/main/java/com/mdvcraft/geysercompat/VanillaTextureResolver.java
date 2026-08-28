package com.mdvcraft.geysercompat;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Convierte IDs Java a rutas de texturas vanilla de Bedrock.
 *
 * Bedrock conserva bastantes nombres historicos (wood_*, gold_*, record_*, etc.)
 * y por eso no alcanza con hacer textures/items/<java_id>. Esta tabla cubre las
 * diferencias comunes de 1.21.x y deja overrides manuales para excepciones.
 */
final class VanillaTextureResolver {
    private static final Map<String, String> EXACT = new HashMap<>();
    private static final Map<String, String> BLOCK_EXACT = new HashMap<>();
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
        alias("salmon", "textures/items/fish_salmon_raw");
        alias("cooked_salmon", "textures/items/fish_salmon_cooked");
        alias("tropical_fish", "textures/items/fish_clownfish_raw");
        alias("pufferfish", "textures/items/fish_pufferfish_raw");
        alias("golden_carrot", "textures/items/carrot_golden");
        alias("melon_slice", "textures/items/melon");
        alias("glistering_melon_slice", "textures/items/melon_speckled");
        alias("popped_chorus_fruit", "textures/items/chorus_fruit_popped");
        alias("baked_potato", "textures/items/potato_baked");
        alias("poisonous_potato", "textures/items/potato_poisonous");
        alias("nether_brick", "textures/items/netherbrick");
        alias("dragon_breath", "textures/items/dragons_breath");
        alias("nautilus_shell", "textures/items/nautilus");
        alias("heart_of_the_sea", "textures/items/heartofthesea_closed");
        alias("fermented_spider_eye", "textures/items/spider_eye_fermented");
        alias("fire_charge", "textures/items/fireball");

        // Libros, botellas y pociones.
        alias("book", "textures/items/book_normal");
        alias("enchanted_book", "textures/items/book_enchanted");
        alias("writable_book", "textures/items/book_writable");
        alias("written_book", "textures/items/book_written");
        alias("glass_bottle", "textures/items/potion_bottle_empty");
        alias("potion", "textures/items/potion_bottle_drinkable");
        alias("splash_potion", "textures/items/potion_bottle_splash");
        alias("lingering_potion", "textures/items/potion_bottle_lingering");

        // Semillas / redstone / tintes: Bedrock conserva nombres legacy.
        alias("sugar_cane", "textures/items/reeds");
        alias("redstone", "textures/items/redstone_dust");
        alias("wheat_seeds", "textures/items/seeds_wheat");
        alias("melon_seeds", "textures/items/seeds_melon");
        alias("pumpkin_seeds", "textures/items/seeds_pumpkin");
        alias("beetroot_seeds", "textures/items/seeds_beetroot");
        alias("ink_sac", "textures/items/dye_powder_black_new");
        alias("lapis_lazuli", "textures/items/dye_powder_blue_new");
        alias("cocoa_beans", "textures/items/dye_powder_brown_new");
        alias("bone_meal", "textures/items/dye_powder_white_new");
        dye("black", "black_new");
        dye("blue", "blue_new");
        dye("brown", "brown_new");
        dye("white", "white_new");
        dye("red", "red");
        dye("green", "green");
        dye("purple", "purple");
        dye("cyan", "cyan");
        dye("light_gray", "silver");
        dye("gray", "gray");
        dye("pink", "pink");
        dye("lime", "lime");
        dye("yellow", "yellow");
        dye("light_blue", "light_blue");
        dye("magenta", "magenta");
        dye("orange", "orange");

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
        alias("shield", "textures/entity/shield");

        // Puertas vanilla con nombres historicos en Bedrock.
        alias("oak_door", "textures/items/door_wood");
        alias("iron_door", "textures/items/door_iron");
        alias("spruce_door", "textures/items/door_spruce");
        alias("birch_door", "textures/items/door_birch");
        alias("jungle_door", "textures/items/door_jungle");
        alias("acacia_door", "textures/items/door_acacia");
        alias("dark_oak_door", "textures/items/door_dark_oak");
        // Las maderas nuevas coinciden con el id Java.
        alias("mangrove_door", "textures/items/mangrove_door");
        alias("cherry_door", "textures/items/cherry_door");
        alias("bamboo_door", "textures/items/bamboo_door");
        alias("crimson_door", "textures/items/crimson_door");
        alias("warped_door", "textures/items/warped_door");
        alias("pale_oak_door", "textures/items/pale_oak_door");

        // Carteles.
        alias("oak_sign", "textures/items/sign");
        alias("spruce_sign", "textures/items/sign_spruce");
        alias("birch_sign", "textures/items/sign_birch");
        alias("jungle_sign", "textures/items/sign_jungle");
        alias("acacia_sign", "textures/items/sign_acacia");
        alias("dark_oak_sign", "textures/items/sign_darkoak");
        alias("crimson_sign", "textures/items/sign_crimson");
        alias("warped_sign", "textures/items/sign_warped");
        alias("mangrove_sign", "textures/items/mangrove_sign");
        alias("bamboo_sign", "textures/items/bamboo_sign");
        alias("cherry_sign", "textures/items/cherry_sign");
        alias("pale_oak_sign", "textures/items/pale_oak_sign");
        hangingSign("oak");
        hangingSign("spruce");
        hangingSign("birch");
        hangingSign("jungle");
        hangingSign("acacia");
        hangingSign("dark_oak");
        hangingSign("crimson");
        hangingSign("warped");
        hangingSign("mangrove");
        hangingSign("bamboo");
        hangingSign("cherry");
        hangingSign("pale_oak");

        // Botes y cofres-bote.
        boat("oak", "oak");
        boat("spruce", "spruce");
        boat("birch", "birch");
        boat("jungle", "jungle");
        boat("acacia", "acacia");
        boat("dark_oak", "darkoak");
        boat("mangrove", "mangrove");
        boat("cherry", "cherry");
        boat("pale_oak", "pale_oak");
        alias("bamboo_raft", "textures/items/bamboo_raft");
        alias("bamboo_chest_raft", "textures/items/bamboo_chest_raft");

        // Minecarts.
        alias("minecart", "textures/items/minecart_normal");
        alias("chest_minecart", "textures/items/minecart_chest");
        alias("furnace_minecart", "textures/items/minecart_furnace");
        alias("tnt_minecart", "textures/items/minecart_tnt");
        alias("hopper_minecart", "textures/items/minecart_hopper");
        alias("command_block_minecart", "textures/items/minecart_command_block");

        // Armadura de caballo.
        alias("golden_horse_armor", "textures/items/gold_horse_armor");

        // Music discs (nombres historicos y nuevos de Bedrock).
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
        alias("music_disc_relic", "textures/items/music_disc_relic");
        alias("music_disc_creator", "textures/items/music_disc_creator");
        alias("music_disc_creator_music_box", "textures/items/music_disc_creator_music_box");
        alias("music_disc_precipice", "textures/items/music_disc_precipice");
        alias("music_disc_tears", "textures/items/music_disc_tears");
        alias("music_disc_lava_chicken", "textures/items/music_disc_lava_chicken");

        // Bloques clasicos cuyo nombre de textura Bedrock no coincide con Java.
        block("grass_block", "textures/blocks/grass_side_carried");
        block("oak_planks", "textures/blocks/planks_oak");
        block("spruce_planks", "textures/blocks/planks_spruce");
        block("birch_planks", "textures/blocks/planks_birch");
        block("jungle_planks", "textures/blocks/planks_jungle");
        block("acacia_planks", "textures/blocks/planks_acacia");
        block("dark_oak_planks", "textures/blocks/planks_big_oak");
        block("oak_log", "textures/blocks/log_oak");
        block("spruce_log", "textures/blocks/log_spruce");
        block("birch_log", "textures/blocks/log_birch");
        block("jungle_log", "textures/blocks/log_jungle");
        block("acacia_log", "textures/blocks/log_acacia");
        block("dark_oak_log", "textures/blocks/log_big_oak");
        block("oak_wood", "textures/blocks/log_oak");
        block("spruce_wood", "textures/blocks/log_spruce");
        block("birch_wood", "textures/blocks/log_birch");
        block("jungle_wood", "textures/blocks/log_jungle");
        block("acacia_wood", "textures/blocks/log_acacia");
        block("dark_oak_wood", "textures/blocks/log_big_oak");
        block("oak_leaves", "textures/blocks/leaves_oak");
        block("spruce_leaves", "textures/blocks/leaves_spruce");
        block("birch_leaves", "textures/blocks/leaves_birch");
        block("jungle_leaves", "textures/blocks/leaves_jungle");
        block("acacia_leaves", "textures/blocks/leaves_acacia");
        block("dark_oak_leaves", "textures/blocks/leaves_big_oak");
        block("bricks", "textures/blocks/brick");
        block("stone_bricks", "textures/blocks/stonebrick");
        block("mossy_stone_bricks", "textures/blocks/stonebrick_mossy");
        block("cracked_stone_bricks", "textures/blocks/stonebrick_cracked");
        block("chiseled_stone_bricks", "textures/blocks/stonebrick_carved");
        block("nether_bricks", "textures/blocks/nether_brick");
        block("red_nether_bricks", "textures/blocks/red_nether_brick");
        block("quartz_block", "textures/blocks/quartz_block_side");
        block("quartz_pillar", "textures/blocks/quartz_block_lines");
        block("chiseled_quartz_block", "textures/blocks/quartz_block_chiseled");
        block("smooth_quartz", "textures/blocks/quartz_block_bottom");
        block("bookshelf", "textures/blocks/bookshelf");
        block("crafting_table", "textures/blocks/crafting_table_front");
        block("furnace", "textures/blocks/furnace_front_off");
        block("blast_furnace", "textures/blocks/blast_furnace_front_off");
        block("smoker", "textures/blocks/smoker_front_off");
        block("chest", "textures/blocks/planks_oak");
        block("ender_chest", "textures/blocks/obsidian");
        block("tnt", "textures/blocks/tnt_side");
        block("hay_block", "textures/blocks/hay_block_side");
        block("dried_kelp_block", "textures/blocks/dried_kelp_side_a");
        block("melon", "textures/blocks/melon_side");
        block("pumpkin", "textures/blocks/pumpkin_side");
        block("carved_pumpkin", "textures/blocks/pumpkin_face_off");
        block("jack_o_lantern", "textures/blocks/pumpkin_face_on");
    }

    private VanillaTextureResolver() {}

    static String resolveIconTexture(String javaId, boolean block, Map<String, String> overrides) {
        String normalized = CompatConfig.normalizeId(javaId);
        String manual = overrides.get(normalized);
        if (manual != null && !manual.isBlank()) return manual;

        String path = normalized.substring(normalized.indexOf(':') + 1);
        // Muchos bloques (puertas, carteles, campfires, etc.) tienen una textura de item
        // dedicada. Si existe en EXACT se prefiere sobre un recorte del terrain atlas.
        String exactItem = EXACT.get(path);
        if (exactItem != null) return exactItem;
        return block ? resolveFlatBlock(normalized, overrides) : resolve(normalized, overrides);
    }

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

        // Spawn eggs modernos de Bedrock usan spawn_eggs/spawn_egg_<mob>.
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

        String exact = BLOCK_EXACT.get(path);
        if (exact != null) return exact;

        // Familias legacy del terrain atlas.
        if (path.endsWith("_wool")) {
            return "textures/blocks/wool_colored_" + path.substring(0, path.length() - "_wool".length());
        }
        if (path.endsWith("_concrete")) {
            return "textures/blocks/concrete_" + path.substring(0, path.length() - "_concrete".length());
        }
        if (path.endsWith("_concrete_powder")) {
            return "textures/blocks/concrete_powder_" + path.substring(0, path.length() - "_concrete_powder".length());
        }
        if (path.endsWith("_terracotta")) {
            String color = path.substring(0, path.length() - "_terracotta".length());
            return "textures/blocks/hardened_clay_stained_" + bedrockColor(color);
        }
        if (path.equals("terracotta")) return "textures/blocks/hardened_clay";

        // Para bloques modernos Mojang suele conservar el id Java como nombre de textura.
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

    private static String bedrockColor(String javaColor) {
        return javaColor.equals("light_gray") ? "silver" : javaColor;
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

    private static void block(String id, String texture) {
        BLOCK_EXACT.put(id, texture);
    }

    private static void dye(String javaColor, String bedrockColor) {
        alias(javaColor + "_dye", "textures/items/dye_powder_" + bedrockColor);
    }

    private static void boat(String javaWood, String bedrockWood) {
        String boat = javaWood.equals("mangrove") || javaWood.equals("cherry") || javaWood.equals("pale_oak")
                ? javaWood + "_boat"
                : "boat_" + bedrockWood;
        alias(javaWood + "_boat", "textures/items/" + boat);
        alias(javaWood + "_chest_boat", "textures/items/" + javaWood + "_chest_boat");
    }

    private static void hangingSign(String wood) {
        alias(wood + "_hanging_sign", "textures/items/" + wood + "_hanging_sign");
    }
}
