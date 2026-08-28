package com.mdvcraft.geysercompat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Parser deliberadamente pequeno para el config.yml incluido con la extension.
 * Evita empaquetar dependencias extra dentro de una Geyser Extension.
 */
final class CompatConfig {
    boolean itemModelsEnabled = true;

    /**
     * Bases "amplias": para cada una se registran TODOS los modelos vanilla.
     *
     * En MDVCRAFT 1.0.3 quedan VACIAS por defecto. Registrar STICK x ~1400 modelos
     * provocaba miles de definiciones innecesarias y Geyser terminaba omitiendo
     * una parte. Ahora se registran solamente los pares que existen de verdad
     * dentro de plugins/MMOItems/item.
     */
    final List<String> baseItems = new ArrayList<>();

    /**
     * Modo recomendado para MDVCRAFT. Ignora las bases amplias antiguas del
     * config 1.0.2 y limita el escaneo a la carpeta real donde viven los items.
     * Esto tambien hace que una config vieja siga siendo segura al actualizar.
     */
    boolean mmoItemsOnlyMode = true;
    String mmoItemsFolder = "plugins/MMOItems/item";

    /**
     * Desde 1.0.2 detectamos pares material+model en configs y registramos solo esos pares.
     * Asi un TRIDENT->DIAMOND_AXE o STONE_PICKAXE->BLAZE_ROD funciona sin meter el material
     * base manualmente en base-items ni explotar la cantidad de custom items.
     */
    boolean autoDetectPairs = true;
    final List<String> pairScanRoots = new ArrayList<>(List.of("plugins/MMOItems/item"));
    final Set<String> pairScanExtensions = new HashSet<>(Set.of("yml", "yaml"));
    int pairScanMaxFileSizeMb = 16;
    final List<String> manualPairs = new ArrayList<>();

    boolean includeBlockItems = true;
    boolean use3dBlockIcons = true;
    // 1.0.7: conservado solo para leer configs viejas. Geyser 2.11 no permite
    // BLOCK_PLACER en definiciones que extienden items vanilla, por lo que el
    // registrador usa siempre fallback visual estable.
    boolean nativeBlockRendering = false;
    final Map<String, String> textureOverrides = new HashMap<>();
    final Map<String, String> blockIdOverrides = new HashMap<>();

    boolean skullsEnabled = true;
    boolean scanServerFiles = true;
    final List<String> scanRoots = new ArrayList<>(List.of("plugins"));
    final Set<String> textExtensions = new HashSet<>(Set.of("yml", "yaml", "json", "txt", "conf", "properties"));
    boolean scanJars = true;
    int maxFileSizeMb = 16;
    int maxJarEntryMb = 4;
    boolean skullCacheEnabled = true;
    boolean rebuildSkullCache = false;
    final List<String> manualProfiles = new ArrayList<>();

    boolean debug = false;

    static CompatConfig load(Path file) throws IOException {
        CompatConfig cfg = new CompatConfig();
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);

        String section = "";
        String key = "";

        boolean baseItemsSeen = false;
        boolean pairRootsSeen = false;
        boolean pairExtSeen = false;
        boolean manualPairsSeen = false;
        boolean rootsSeen = false;
        boolean extSeen = false;
        boolean manualSeen = false;

        for (String raw : lines) {
            String noComment = stripComment(raw);
            if (noComment.isBlank()) continue;

            int indent = leadingSpaces(noComment);
            String line = noComment.trim();

            if (indent == 0 && line.endsWith(":")) {
                section = line.substring(0, line.length() - 1).trim();
                key = "";
                continue;
            }

            // Propiedades simples de raiz (por ejemplo debug: false).
            if (indent == 0 && line.contains(":") && !line.endsWith(":")) {
                int split = line.indexOf(':');
                String rootKey = line.substring(0, split).trim();
                String rootValue = line.substring(split + 1).trim();
                section = "";
                key = rootKey;
                if (rootKey.equals("debug")) cfg.debug = bool(rootValue, cfg.debug);
                continue;
            }

            if (indent <= 2 && line.contains(":")) {
                int split = line.indexOf(':');
                key = line.substring(0, split).trim();
                String value = line.substring(split + 1).trim();

                if (section.equals("item-models")) {
                    switch (key) {
                        case "enabled" -> cfg.itemModelsEnabled = bool(value, cfg.itemModelsEnabled);
                        case "include-block-items" -> cfg.includeBlockItems = bool(value, cfg.includeBlockItems);
                        case "use-3d-block-icons" -> cfg.use3dBlockIcons = bool(value, cfg.use3dBlockIcons);
                        case "native-block-rendering" -> cfg.nativeBlockRendering = bool(value, cfg.nativeBlockRendering);
                        case "auto-detect-pairs" -> cfg.autoDetectPairs = bool(value, cfg.autoDetectPairs);
                        case "mmoitems-only-mode" -> cfg.mmoItemsOnlyMode = bool(value, cfg.mmoItemsOnlyMode);
                        case "mmoitems-folder" -> cfg.mmoItemsFolder = unquote(value);
                        case "pair-scan-max-file-size-mb" -> cfg.pairScanMaxFileSizeMb = integer(value, cfg.pairScanMaxFileSizeMb);
                        case "base-items" -> {
                            if (!baseItemsSeen) {
                                cfg.baseItems.clear();
                                baseItemsSeen = true;
                            }
                        }
                        case "pair-scan-roots" -> {
                            if (!pairRootsSeen) {
                                cfg.pairScanRoots.clear();
                                pairRootsSeen = true;
                            }
                        }
                        case "pair-scan-extensions" -> {
                            if (!pairExtSeen) {
                                cfg.pairScanExtensions.clear();
                                pairExtSeen = true;
                            }
                        }
                        case "manual-pairs" -> {
                            if (!manualPairsSeen) {
                                cfg.manualPairs.clear();
                                manualPairsSeen = true;
                            }
                        }
                    }
                } else if (section.equals("skulls")) {
                    switch (key) {
                        case "enabled" -> cfg.skullsEnabled = bool(value, cfg.skullsEnabled);
                        case "scan-server-files" -> cfg.scanServerFiles = bool(value, cfg.scanServerFiles);
                        case "scan-jars" -> cfg.scanJars = bool(value, cfg.scanJars);
                        case "max-file-size-mb" -> cfg.maxFileSizeMb = integer(value, cfg.maxFileSizeMb);
                        case "max-jar-entry-mb" -> cfg.maxJarEntryMb = integer(value, cfg.maxJarEntryMb);
                        case "cache-enabled" -> cfg.skullCacheEnabled = bool(value, cfg.skullCacheEnabled);
                        case "rebuild-cache" -> cfg.rebuildSkullCache = bool(value, cfg.rebuildSkullCache);
                        case "scan-roots" -> {
                            if (!rootsSeen) {
                                cfg.scanRoots.clear();
                                rootsSeen = true;
                            }
                        }
                        case "text-extensions" -> {
                            if (!extSeen) {
                                cfg.textExtensions.clear();
                                extSeen = true;
                            }
                        }
                        case "manual-profiles" -> {
                            if (!manualSeen) {
                                cfg.manualProfiles.clear();
                                manualSeen = true;
                            }
                        }
                    }
                } else if (section.isEmpty() && key.equals("debug")) {
                    cfg.debug = bool(value, cfg.debug);
                }
                continue;
            }

            if (line.startsWith("- ")) {
                String value = unquote(line.substring(2).trim());
                if (value.isBlank()) continue;

                if (section.equals("item-models")) {
                    switch (key) {
                        case "base-items" -> cfg.baseItems.add(normalizeId(value));
                        case "pair-scan-roots" -> cfg.pairScanRoots.add(value);
                        case "pair-scan-extensions" -> cfg.pairScanExtensions.add(value.toLowerCase(Locale.ROOT).replace(".", ""));
                        case "manual-pairs" -> cfg.manualPairs.add(value);
                        case "texture-overrides" -> putPair(cfg.textureOverrides, value);
                        case "block-id-overrides" -> putPair(cfg.blockIdOverrides, value);
                    }
                } else if (section.equals("skulls")) {
                    switch (key) {
                        case "scan-roots" -> cfg.scanRoots.add(value);
                        case "text-extensions" -> cfg.textExtensions.add(value.toLowerCase(Locale.ROOT).replace(".", ""));
                        case "manual-profiles" -> cfg.manualProfiles.add(value);
                    }
                }
            }
        }

        /*
         * Compatibilidad de actualizacion:
         * una config 1.0.2 puede seguir teniendo base-items: STICK/APPLE y
         * pair-scan-roots: plugins. Con mmoitems-only-mode=true no queremos
         * volver a registrar miles de definiciones ni escanear 1500+ archivos.
         */
        if (cfg.mmoItemsOnlyMode) {
            cfg.baseItems.clear();
            cfg.pairScanRoots.clear();
            cfg.pairScanRoots.add(cfg.mmoItemsFolder == null || cfg.mmoItemsFolder.isBlank()
                    ? "plugins/MMOItems/item"
                    : cfg.mmoItemsFolder);
            cfg.pairScanExtensions.clear();
            cfg.pairScanExtensions.add("yml");
            cfg.pairScanExtensions.add("yaml");
        } else if (cfg.pairScanRoots.isEmpty()) {
            cfg.pairScanRoots.add("plugins/MMOItems/item");
        }

        if (cfg.scanRoots.isEmpty()) cfg.scanRoots.add("plugins");
        return cfg;
    }

    private static void putPair(Map<String, String> map, String value) {
        int split = value.indexOf('=');
        if (split <= 0 || split >= value.length() - 1) return;
        String left = normalizeId(unquote(value.substring(0, split).trim()));
        String right = unquote(value.substring(split + 1).trim());
        map.put(left, right);
    }

    static String normalizeId(String value) {
        String v = unquote(value.trim()).toLowerCase(Locale.ROOT);
        return v.contains(":") ? v : "minecraft:" + v;
    }

    private static boolean bool(String value, boolean fallback) {
        if (value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("false")) return false;
        return fallback;
    }

    private static int integer(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int leadingSpaces(String value) {
        int i = 0;
        while (i < value.length() && value.charAt(i) == ' ') i++;
        return i;
    }

    private static String stripComment(String value) {
        boolean single = false;
        boolean doub = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\'' && !doub) single = !single;
            else if (c == '"' && !single) doub = !doub;
            else if (c == '#' && !single && !doub) return value.substring(0, i);
        }
        return value;
    }

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
