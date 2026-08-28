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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class MDVGeyserCompat implements Extension {
    private CompatConfig config;
    private List<VanillaMaterialRegistry.Entry> vanillaTargets = Collections.emptyList();
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
                generatedPack = ResourcePackGenerator.generate(dataFolder(), config, vanillaTargets);
                logger().info("Detectados " + vanillaTargets.size() + " materiales vanilla utilizables como item_model.");
            } catch (ReflectiveOperationException e) {
                logger().error("No pude leer org.bukkit.Material. Esta build esta pensada para Geyser-Spigot/Purpur.", e);
                vanillaTargets = Collections.emptyList();
            } catch (IOException e) {
                logger().error("No pude generar el resource pack automatico de item_model.", e);
            }
        }

        if (config.skullsEnabled) {
            SkullScanner.Result result = SkullScanner.scan(serverRoot, config);
            skullProfiles = Set.copyOf(result.profiles());
            try {
                Files.write(dataFolder().resolve("skulls-found.txt"), skullProfiles.stream().sorted().toList());
            } catch (IOException ignored) {
            }
            logger().info("Skulls Base64: " + skullProfiles.size() + " perfiles detectados ("
                    + result.filesScanned() + " archivos, " + result.jarsScanned() + " jars, "
                    + result.millis() + " ms).");
        }

        if (config.debug) {
            logger().info("Bases item_model: " + config.baseItems);
            logger().info("Preparacion total: " + (System.currentTimeMillis() - started) + " ms.");
        }
    }

    @Subscribe
    public void onDefineCustomItems(GeyserDefineCustomItemsEvent event) {
        if (config == null || !config.itemModelsEnabled || vanillaTargets.isEmpty()) return;

        int registered = 0;
        int failed = 0;
        for (String baseRaw : config.baseItems) {
            String base = CompatConfig.normalizeId(baseRaw);
            Identifier baseId;
            try {
                baseId = Identifier.of(base);
            } catch (Exception e) {
                logger().warning("Base item invalida en config: " + base);
                continue;
            }

            for (VanillaMaterialRegistry.Entry target : vanillaTargets) {
                if (!config.includeBlockItems && target.block()) continue;
                if (target.id().equals(base)) continue;

                try {
                    String bedrockCustomId = VanillaTextureResolver.bedrockIdentifier(base, target.id());
                    Identifier targetModel = Identifier.of(target.id());

                    CustomItemDefinition.Builder definition = CustomItemDefinition.builder(
                            Identifier.of(bedrockCustomId),
                            targetModel
                    );

                    // Geyser exige al menos un predicate para item_model del namespace minecraft.
                    // count(1) es verdadero para cualquier stack real y no exige modificar el item Java.
                    definition.predicate(ItemRangeDispatchPredicate.count(1));

                    CustomItemBedrockOptions.Builder bedrock = CustomItemBedrockOptions.builder()
                            .allowOffhand(true)
                            .displayHandheld(VanillaTextureResolver.displayHandheld(target.id()));

                    if (target.block() && config.use3dBlockIcons) {
                        String bedrockBlock = config.blockIdOverrides.getOrDefault(target.id(), target.id());
                        definition.component(
                                GeyserItemDataComponents.BLOCK_PLACER,
                                GeyserBlockPlacer.of(Identifier.of(bedrockBlock), true)
                        );
                    } else {
                        bedrock.icon(VanillaTextureResolver.iconKey(base, target.id()));
                    }

                    definition.bedrockOptions(bedrock);
                    event.register(baseId, definition.build());
                    registered++;
                } catch (Throwable throwable) {
                    failed++;
                    if (config.debug) {
                        logger().warning("No se pudo registrar " + base + " -> " + target.id() + ": " + throwable.getMessage());
                    }
                }
            }
        }

        logger().info("item_model: " + registered + " definiciones Bedrock registradas"
                + (failed > 0 ? " (" + failed + " omitidas)" : "") + ".");
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
