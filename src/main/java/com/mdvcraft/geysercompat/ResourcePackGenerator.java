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
    private static final UUID HEADER_UUID = UUID.fromString("d5bd9e27-2a93-4f50-8c4f-49b5f4bb1c41");
    private static final UUID MODULE_UUID = UUID.fromString("afe301ef-ec6f-4df0-8c70-64c4c033a81f");

    private ResourcePackGenerator() {}

    static Path generate(Path dataFolder,
                         CompatConfig cfg,
                         List<VanillaMaterialRegistry.Entry> targets) throws IOException {
        Files.createDirectories(dataFolder);
        Path pack = dataFolder.resolve("MDVGeyserCompat-AutoPack.mcpack");

        // TreeMap = salida estable entre reinicios.
        Map<String, String> textureData = new TreeMap<>();
        List<String> report = new ArrayList<>();
        for (String base : cfg.baseItems) {
            for (VanillaMaterialRegistry.Entry target : targets) {
                if (!cfg.includeBlockItems && target.block()) continue;
                if (target.id().equals(base)) continue;

                // 1.0.0 no generaba icono para bloques si use-3d-block-icons=true.
                // Eso podia verse correcto en mano/mundo por block_placer, pero en GUIs
                // quedaba sin icono y Bedrock mostraba missing texture. Desde 1.0.1
                // TODOS los mappings tienen icono de inventario.
                String icon = VanillaTextureResolver.iconKey(base, target.id());
                String texture = VanillaTextureResolver.resolveIconTexture(
                        target.id(), target.block(), cfg.textureOverrides);
                textureData.put(icon, texture);
                report.add(base + " -> " + target.id() + " = " + texture
                        + (target.block() ? " [block]" : ""));
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
                    "name": "MDVGeyserCompat AutoPack",
                    "description": "Generated automatically by MDVGeyserCompat",
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

        // Muy util para localizar cualquier excepcion de Mojang sin adivinar.
        Files.write(dataFolder.resolve("item-texture-report.txt"), report, StandardCharsets.UTF_8);
        return pack;
    }

    private static String buildItemTextureJson(Map<String, String> entries) {
        StringBuilder out = new StringBuilder(128 * 1024);
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
