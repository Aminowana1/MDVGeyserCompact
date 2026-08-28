package com.mdvcraft.geysercompat;

import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomItemsEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomSkullsEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineResourcePacksEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPreInitializeEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.item.custom.v2.CustomItemBedrockOptions;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.item.custom.v2.component.geyser.GeyserBlockPlacer;
import org.geysermc.geyser.api.item.custom.v2.component.geyser.GeyserItemDataComponents;
import org.geysermc.geyser.api.predicate.item.ItemRangeDispatchPredicate;
import org.geysermc.geyser.api.pack.PackCodec;
import org.geysermc.geyser.api.pack.ResourcePack;
import org.geysermc.geyser.api.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MDVGeyserCompat implements Extension {
    private CompatConfig config;
    private List<VanillaMaterialRegistry.Entry> vanillaTargets = Collections.emptyList();
    private Map<String, List<VanillaMaterialRegistry.Entry>> itemModelPlan = Collections.emptyMap();
    private Set<String> skullProfiles = Collections.emptySet();
    private Path generatedPack;
    private Path serverRoot;

    @Subscribe
    public void onPreInitialize(GeyserPreInitializeEvent event) {
        long started = System.currentTimeMillis();
        serverRoot = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();

        try {
            Files.createDirectories(dataFolder());
            Path configPath = dataFolder().resolve("config.yml");
            if (Files.notExists(configPath)) copyDefault("config.yml", configPath);
            config = CompatConfig.load(configPath);
        } catch (Exception e) {
            logger().error("No se pudo cargar config.yml; se usaran valores por defecto.", e);
            config = new CompatConfig();
        }

        if (config.itemModelsEnabled) {
            try {
                vanillaTargets = VanillaMaterialRegistry.discover();
                itemModelPlan = buildItemModelPlan(vanillaTargets);
                generatedPack = ResourcePackGenerator.generate(dataFolder(), config, itemModelPlan);

                int definitions = itemModelPlan.values().stream().mapToInt(List::size).sum();
                logger().info("Detectados " + vanillaTargets.size() + " materiales vanilla utilizables como item_model.");
                logger().info("item_model: " + definitions + " combinaciones preparadas para "
                        + itemModelPlan.size() + " materiales base.");
            } catch (ReflectiveOperationException e) {
                logger().error("No pude leer org.bukkit.Material. Esta build esta pensada para Geyser-Spigot/Purpur.", e);
                vanillaTargets = Collections.emptyList();
                itemModelPlan = Collections.emptyMap();
            } catch (IOException e) {
                logger().error("No pude generar el resource pack automatico de item_model.", e);
            }
        }

        if (config.skullsEnabled) {
            SkullScanner.Result result = SkullScanner.loadOrScan(serverRoot, dataFolder(), config);
            skullProfiles = Set.copyOf(result.profiles());
            if (result.fromCache()) {
                logger().info("Skulls Base64: " + skullProfiles.size()
                        + " perfiles cargados desde cache (" + result.millis() + " ms).");
            } else {
                logger().info("Skulls Base64: " + skullProfiles.size() + " perfiles detectados ("
                        + result.filesScanned() + " archivos, " + result.jarsScanned() + " jars, "
                        + result.millis() + " ms). Cache guardada.");
            }
        }

        if (config.debug) {
            logger().info("Bases amplias item_model: " + config.baseItems);
            logger().info("Preparacion total: " + (System.currentTimeMillis() - started) + " ms.");
        }
    }

    /**
     * 1.0.3: MDVCRAFT usa deteccion exacta desde plugins/MMOItems/item.
     * Las bases amplias quedan disponibles solo si mmoitems-only-mode=false.
     * Esto evita registrar miles de definiciones innecesarias y que Geyser
     * termine omitiendo modelos que el servidor realmente usa.
     */
    private Map<String, List<VanillaMaterialRegistry.Entry>> buildItemModelPlan(
            List<VanillaMaterialRegistry.Entry> targets) throws IOException {

        Map<String, VanillaMaterialRegistry.Entry> byId = new HashMap<>();
        Set<String> validItems = new LinkedHashSet<>();
        for (VanillaMaterialRegistry.Entry entry : targets) {
            byId.put(entry.id(), entry);
            validItems.add(entry.id());
        }

        Map<String, LinkedHashSet<VanillaMaterialRegistry.Entry>> mutable = new LinkedHashMap<>();

        // Bases amplias: se les permite verse como cualquier item/bloque vanilla.
        for (String rawBase : config.baseItems) {
            String base = CompatConfig.normalizeId(rawBase);
            if (!validItems.contains(base)) {
                logger().warning("Base item_model desconocida/invalidada: " + base);
                continue;
            }

            LinkedHashSet<VanillaMaterialRegistry.Entry> entries =
                    mutable.computeIfAbsent(base, ignored -> new LinkedHashSet<>());
            for (VanillaMaterialRegistry.Entry target : targets) {
                if (!config.includeBlockItems && target.block()) continue;
                if (!target.id().equals(base)) entries.add(target);
            }
        }

        // Pares exactos detectados en YAML/JSON/TXT de los plugins.
        ItemModelPairScanner.Result scanned = ItemModelPairScanner.scan(serverRoot, config, validItems);

        // 1.0.5: fallback MDVCRAFT para modelos vanilla que sabemos que existen
        // en MMOItems. Se deduplican con el escaneo normal. Esto evita que un
        // parser/config antiguo deje fuera justamente los pares mas usados.
        Map<String, Set<String>> detectedPairs = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> e : scanned.pairs().entrySet()) {
            detectedPairs.put(e.getKey(), new LinkedHashSet<>(e.getValue()));
        }
        forcePair(detectedPairs, validItems, "minecraft:stick", "minecraft:kelp");
        forcePair(detectedPairs, validItems, "minecraft:stone_pickaxe", "minecraft:bamboo");
        forcePair(detectedPairs, validItems, "minecraft:stick", "minecraft:moss_block");
        forcePair(detectedPairs, validItems, "minecraft:stick", "minecraft:lightning_rod");
        forcePair(detectedPairs, validItems, "minecraft:stick", "minecraft:chain");
        forcePair(detectedPairs, validItems, "minecraft:stick", "minecraft:coal_block");
        forcePair(detectedPairs, validItems, "minecraft:stick", "minecraft:obsidian");
        forcePair(detectedPairs, validItems, "minecraft:stick", "minecraft:ochre_froglight");
        forcePair(detectedPairs, validItems, "minecraft:stick", "minecraft:pearlescent_froglight");
        forcePair(detectedPairs, validItems, "minecraft:stick", "minecraft:verdant_froglight");
        forcePair(detectedPairs, validItems, "minecraft:stick", "minecraft:redstone_block");
        forcePair(detectedPairs, validItems, "minecraft:stick", "minecraft:scute");
        for (Map.Entry<String, Set<String>> pair : detectedPairs.entrySet()) {
            String base = pair.getKey();
            if (!validItems.contains(base)) continue;

            LinkedHashSet<VanillaMaterialRegistry.Entry> entries =
                    mutable.computeIfAbsent(base, ignored -> new LinkedHashSet<>());
            for (String targetId : pair.getValue()) {
                VanillaMaterialRegistry.Entry target = byId.get(targetId);
                if (target == null) {
                    // item_model es un recurso, no necesariamente un Material Bukkit.
                    // Mantener el ID exacto permite casos vanilla/legacy como minecraft:scute.
                    target = new VanillaMaterialRegistry.Entry(targetId, false, false);
                }
                if (!config.includeBlockItems && target.block()) continue;
                if (!target.id().equals(base)) entries.add(target);
            }
        }

        writePairReport(scanned, detectedPairs);
        logger().info("item_model: " + scanned.pairCount() + " pares base->modelo detectados en "
                + scanned.filesScanned() + " archivos (" + scanned.millis() + " ms)." );

        // Orden estable = pack/report reproducible y menos cache churn del cliente Bedrock.
        Map<String, List<VanillaMaterialRegistry.Entry>> stable = new LinkedHashMap<>();
        mutable.keySet().stream().sorted().forEach(base -> {
            List<VanillaMaterialRegistry.Entry> entries = new ArrayList<>(mutable.get(base));
            entries.sort(Comparator.comparing(VanillaMaterialRegistry.Entry::id));
            if (!entries.isEmpty()) stable.put(base, List.copyOf(entries));
        });
        return Collections.unmodifiableMap(stable);
    }

    private static void forcePair(Map<String, Set<String>> pairs, Set<String> validItems, String base, String target) {
        if (!validItems.contains(base)) return;
        pairs.computeIfAbsent(base, ignored -> new LinkedHashSet<>()).add(target);
    }

    private void writePairReport(ItemModelPairScanner.Result scanned, Map<String, Set<String>> pairs) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("# MDVGeyserCompat 1.0.5 - pares item_model detectados automaticamente");
        lines.add("# Formato: BASE -> MODELO");
        lines.add("");
        for (Map.Entry<String, Set<String>> entry : pairs.entrySet()) {
            for (String target : entry.getValue()) {
                lines.add(entry.getKey() + " -> " + target);
            }
        }
        Files.write(dataFolder().resolve("item-model-pairs-report.txt"), lines, StandardCharsets.UTF_8);
    }

    @Subscribe
    public void onDefineCustomItems(GeyserDefineCustomItemsEvent event) {
        if (config == null || !config.itemModelsEnabled || itemModelPlan.isEmpty()) return;

        int registered = 0;
        int failed = 0;
        List<String> failureReport = new ArrayList<>();
        List<String> registrationReport = new ArrayList<>();

        for (Map.Entry<String, List<VanillaMaterialRegistry.Entry>> planned : itemModelPlan.entrySet()) {
            String base = planned.getKey();
            Identifier baseId;
            try {
                baseId = Identifier.of(base);
            } catch (Exception e) {
                logger().warning("Base item invalida: " + base);
                continue;
            }

            for (VanillaMaterialRegistry.Entry target : planned.getValue()) {
                try {
                    String bedrockCustomId = VanillaTextureResolver.bedrockIdentifier(base, target.id());
                    Identifier targetModel = Identifier.of(target.id());

                    CustomItemDefinition.Builder definition = CustomItemDefinition.builder(
                            Identifier.of(bedrockCustomId),
                            targetModel
                    );

                    /*
                     * IMPORTANTE: Geyser v2 exige predicate cuando el item_model
                     * esta en el namespace minecraft. Estos modelos (kelp, bamboo,
                     * moss_block, chain, redstone_block, etc.) son precisamente
                     * minecraft:*. count(1) coincide con cualquier stack real sin
                     * modificar el ItemStack Java.
                     */
                    if (target.id().startsWith("minecraft:")) {
                        definition.predicate(ItemRangeDispatchPredicate.count(1));
                    }

                    /*
                     * 1.0.6: si el item_model apunta a un bloque vanilla, no usamos
                     * una textura 2D. Geyser expone GeyserBlockPlacer#useBlockIcon,
                     * que hace que Bedrock renderice el bloque 3D nativo como icono.
                     * Esto corrige cubos planos, bamboo estirado y lightning_rod
                     * recortado/desplazado.
                     */
                    boolean nativeBlock3d = target.block() && config.nativeBlockRendering;

                    CustomItemBedrockOptions.Builder bedrock = CustomItemBedrockOptions.builder()
                            .allowOffhand(true)
                            .displayHandheld(!nativeBlock3d && VanillaTextureResolver.displayHandheld(target.id()));

                    String registrationMode;
                    if (nativeBlock3d) {
                        String bedrockBlockId = config.blockIdOverrides.getOrDefault(target.id(), target.id());
                        definition.component(
                                GeyserItemDataComponents.BLOCK_PLACER,
                                GeyserBlockPlacer.of(Identifier.of(bedrockBlockId), true)
                        );
                        // No configuramos icon(): cuando useBlockIcon=true Geyser
                        // omite minecraft:icon para que Bedrock use el modelo 3D.
                        registrationMode = "NATIVE_BLOCK_3D(" + bedrockBlockId + ")";
                    } else {
                        String vanillaAtlas = VanillaTextureResolver.vanillaAtlasIconKey(target.id());
                        String icon = vanillaAtlas != null
                                ? vanillaAtlas
                                : VanillaTextureResolver.iconKey(base, target.id());
                        bedrock.icon(icon);
                        registrationMode = vanillaAtlas != null ? "VANILLA_ATLAS" : "EXPLICIT_TEXTURE";
                    }

                    definition.bedrockOptions(bedrock);
                    event.register(baseId, definition.build());
                    registered++;
                    registrationReport.add(base + " -> " + target.id() + " :: OK :: " + registrationMode);
                } catch (Throwable throwable) {
                    failed++;
                    String reason = throwable.getClass().getSimpleName() + ": "
                            + String.valueOf(throwable.getMessage());
                    failureReport.add(base + " -> " + target.id() + " :: " + reason);
                    registrationReport.add(base + " -> " + target.id() + " :: FAIL :: " + reason);
                    if (config.debug) {
                        logger().warning("No se pudo registrar " + base + " -> " + target.id()
                                + ": " + throwable.getMessage());
                    }
                }
            }
        }

        try {
            List<String> report = new ArrayList<>();
            report.add("# MDVGeyserCompat 1.0.6 - definiciones que Geyser no pudo registrar");
            report.add("# Si queda vacio debajo de esta linea, no hubo omisiones.");
            report.add("");
            report.addAll(failureReport);
            Files.write(dataFolder().resolve("item-model-failures-report.txt"), report, StandardCharsets.UTF_8);

            List<String> reg = new ArrayList<>();
            reg.add("# MDVGeyserCompat 1.0.6 - resultado de registro por item_model");
            reg.add("# Modos: NATIVE_BLOCK_3D, VANILLA_ATLAS, EXPLICIT_TEXTURE");
            reg.add("");
            reg.addAll(registrationReport);
            Files.write(dataFolder().resolve("item-model-registration-report.txt"), reg, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }

        logger().info("item_model: " + registered + " definiciones Bedrock registradas"
                + (failed > 0 ? " (" + failed + " omitidas; ver item-model-failures-report.txt)" : "") + ".");
    }

    @Subscribe
    public void onDefineCustomSkulls(GeyserDefineCustomSkullsEvent event) {
        if (config == null || !config.skullsEnabled || skullProfiles.isEmpty()) return;

        int registered = 0;
        for (String profile : skullProfiles) {
            try {
                event.register(profile, GeyserDefineCustomSkullsEvent.SkullTextureType.PROFILE);
                registered++;
            } catch (Throwable throwable) {
                if (config.debug) logger().warning("Skull omitido: " + throwable.getMessage());
            }
        }
        logger().info("Custom skulls: " + registered + " perfiles Base64 registrados en Geyser.");
    }

    @Subscribe
    public void onDefineResourcePacks(GeyserDefineResourcePacksEvent event) {
        if (generatedPack == null || Files.notExists(generatedPack)) return;
        try {
            event.register(ResourcePack.create(PackCodec.path(generatedPack)));
            logger().info("Resource pack automatico registrado: " + generatedPack.getFileName());
        } catch (Throwable throwable) {
            logger().error("No se pudo registrar el resource pack automatico.", throwable);
        }
    }

    @Subscribe
    public void onPostInitialize(GeyserPostInitializeEvent event) {
        logger().info("MDVGeyserCompat listo. Reinicio completo requerido cuando cambies item_model/skulls.");
    }

    private void copyDefault(String resource, Path destination) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resource)) {
            if (in == null) throw new IOException("Recurso interno no encontrado: " + resource);
            Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
