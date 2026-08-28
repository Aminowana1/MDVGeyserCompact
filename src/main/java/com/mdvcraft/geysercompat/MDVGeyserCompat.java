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
import org.geysermc.geyser.api.pack.PackCodec;
import org.geysermc.geyser.api.pack.ResourcePack;
import org.geysermc.geyser.api.predicate.item.ItemRangeDispatchPredicate;
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
        for (Map.Entry<String, Set<String>> pair : scanned.pairs().entrySet()) {
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

        writePairReport(scanned);
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

    private void writePairReport(ItemModelPairScanner.Result scanned) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("# MDVGeyserCompat 1.0.3 - pares item_model detectados automaticamente");
        lines.add("# Formato: BASE -> MODELO");
        lines.add("");
        for (Map.Entry<String, Set<String>> entry : scanned.pairs().entrySet()) {
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

                    // El modelo minecraft:* requiere predicate. count(1) es verdadero
                    // para cualquier stack real y no requiere modificar el item Java.
                    definition.predicate(ItemRangeDispatchPredicate.count(1));

                    CustomItemBedrockOptions.Builder bedrock = CustomItemBedrockOptions.builder()
                            .allowOffhand(true)
                            .displayHandheld(VanillaTextureResolver.displayHandheld(target.id()))
                            .icon(VanillaTextureResolver.iconKey(base, target.id()));

                    /*
                     * IMPORTANTE 1.0.3:
                     * Girasoles, bambu, flores, tripwire hook y otros bloques no
                     * solidos NO deben usar el block icon 3D de Bedrock. Hacerlo
                     * producia modelos equivocados/missing texture, especialmente
                     * en menus. Solo bloques solidos conservan el render 3D.
                     */
                    if (target.block() && target.solid() && config.use3dBlockIcons) {
                        String bedrockBlock = config.blockIdOverrides.getOrDefault(target.id(), target.id());
                        definition.component(
                                GeyserItemDataComponents.BLOCK_PLACER,
                                GeyserBlockPlacer.of(Identifier.of(bedrockBlock), true)
                        );
                    }

                    definition.bedrockOptions(bedrock);
                    event.register(baseId, definition.build());
                    registered++;
                } catch (Throwable throwable) {
                    failed++;
                    String reason = throwable.getClass().getSimpleName() + ": "
                            + String.valueOf(throwable.getMessage());
                    failureReport.add(base + " -> " + target.id() + " :: " + reason);
                    if (config.debug) {
                        logger().warning("No se pudo registrar " + base + " -> " + target.id()
                                + ": " + throwable.getMessage());
                    }
                }
            }
        }

        try {
            List<String> report = new ArrayList<>();
            report.add("# MDVGeyserCompat 1.0.3 - definiciones que Geyser no pudo registrar");
            report.add("# Si queda vacio debajo de esta linea, no hubo omisiones.");
            report.add("");
            report.addAll(failureReport);
            Files.write(dataFolder().resolve("item-model-failures-report.txt"), report, StandardCharsets.UTF_8);
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
