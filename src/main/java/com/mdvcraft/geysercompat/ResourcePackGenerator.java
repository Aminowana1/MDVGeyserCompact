package com.mdvcraft.geysercompat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

final class ResourcePackGenerator {
    private static final UUID HEADER_UUID = UUID.fromString("8ff6b9ee-4f93-4b48-a896-4cc4be7d8a14");
    private static final UUID MODULE_UUID = UUID.fromString("b6c33cc3-b352-4bd7-a3da-90b5b2f66d2e");

    private ResourcePackGenerator() {}

    static Path generate(Path dataFolder,
                         CompatConfig cfg,
                         Map<String, List<VanillaMaterialRegistry.Entry>> plan) throws IOException {
        Files.createDirectories(dataFolder);
        Path pack = dataFolder.resolve("MDVGeyserCompat-AutoPack.mcpack");

        // TreeMap = salida estable entre reinicios.
        Map<String, String> textureData = new TreeMap<>();
        List<String> report = new ArrayList<>();

        for (Map.Entry<String, List<VanillaMaterialRegistry.Entry>> planned : plan.entrySet()) {
            String base = planned.getKey();
            for (VanillaMaterialRegistry.Entry target : planned.getValue()) {
                String mode = VanillaTextureResolver.appearanceMode(target, cfg.use3dBlockIcons);

                if (mode.equals("NATIVE_BLOCK")) {
                    report.add(base + " -> " + target.id() + " = [native Bedrock block icon]");
                    continue;
                }

                String vanillaAtlas = VanillaTextureResolver.vanillaAtlasIconKey(target.id());
                if (vanillaAtlas != null) {
                    report.add(base + " -> " + target.id() + " = [vanilla atlas key: " + vanillaAtlas + "]");
                    continue;
                }

                String icon = VanillaTextureResolver.iconKey(base, target.id());
                String texture = VanillaTextureResolver.resolveIconTexture(
                        target.id(), target.block(), cfg.textureOverrides);
                textureData.put(icon, texture);
                report.add(base + " -> " + target.id() + " = " + texture
                        + (target.block() ? (target.solid() ? " [block-solid-fallback]" : " [block-flat-fallback]") : ""));
            }
        }

        String itemTexture = buildItemTextureJson(textureData);
        CRC32 crc = new CRC32();
        crc.update(itemTexture.getBytes(StandardCharsets.UTF_8));
        long value = crc.getValue();
        int v1 = (int) ((value >>> 16) & 0x7FFF);
        int v2 = (int) (value & 0x7FFF);
        if (v1 == 0) v1 = 1;
        if (v2 == 0) v2 = 1;

        String manifest = """
                {
                  "format_version": 2,
                  "header": {
                    "name": "MDVGeyserCompat AutoPack 1.0.4",
                    "description": "Generated automatically by MDVGeyserCompat 1.0.4",
                    "uuid": "%s",
                    "version": [1, %d, %d],
                    "min_engine_version": [1, 21, 0]
                  },
                  "modules": [
                    {
                      "type": "resources",
                      "uuid": "%s",
                      "version": [1, %d, %d]
                    }
                  ]
                }
                """.formatted(HEADER_UUID, v1, v2, MODULE_UUID, v1, v2);

        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(pack), StandardCharsets.UTF_8)) {
            put(zip, "manifest.json", manifest);
            put(zip, "textures/item_texture.json", itemTexture);
        }

        Files.write(dataFolder.resolve("item-texture-report.txt"), report, StandardCharsets.UTF_8);
        return pack;
    }

    private static String buildItemTextureJson(Map<String, String> entries) {
        StringBuilder out = new StringBuilder(Math.max(16 * 1024, entries.size() * 100));
        out.append("{\n")
                .append("  \"resource_pack_name\": \"MDVGeyserCompat\",\n")
                .append("  \"texture_name\": \"atlas.items\",\n")
                .append("  \"texture_data\": {\n");

        int i = 0;
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            if (i++ > 0) out.append(",\n");
            out.append("    \"").append(json(entry.getKey())).append("\": {\"textures\": \"")
                    .append(json(entry.getValue())).append("\"}");
        }
        out.append("\n  }\n}\n");
        return out.toString();
    }

    private static void put(ZipOutputStream zip, String name, String content) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        entry.setTime(0L);
        zip.putNextEntry(entry);
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String json(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
