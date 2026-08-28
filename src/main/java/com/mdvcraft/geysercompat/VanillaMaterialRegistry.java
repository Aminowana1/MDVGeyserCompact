package com.mdvcraft.geysercompat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

final class VanillaMaterialRegistry {
    /**
     * solid solo se usa para decidir si conviene pedir a Bedrock el icono/render
     * 3D de bloque. Plantas, tripwire, flores, etc. deben quedar como item 2D.
     */
    record Entry(String id, boolean block, boolean solid) {}

    private VanillaMaterialRegistry() {}

    static List<Entry> discover() throws ReflectiveOperationException {
        Class<?> materialClass = Class.forName("org.bukkit.Material");
        Object[] values = (Object[]) materialClass.getMethod("values").invoke(null);
        Method isItem = materialClass.getMethod("isItem");
        Method isBlock = materialClass.getMethod("isBlock");
        Method getKey = materialClass.getMethod("getKey");

        Method isSolid = null;
        try {
            isSolid = materialClass.getMethod("isSolid");
        } catch (NoSuchMethodException ignored) {
            // Compatibilidad defensiva con APIs Bukkit futuras/raras.
        }

        List<Entry> result = new ArrayList<>();
        for (Object material : values) {
            // Filtra LEGACY antes de invocar metodos Bukkit: CraftBukkit puede
            // inicializar todo el soporte legacy al tocar esos enum values.
            String enumName = ((Enum<?>) material).name();
            if (enumName.startsWith("LEGACY_") || enumName.equals("AIR") || enumName.endsWith("_AIR")) continue;
            if (!(Boolean) isItem.invoke(material)) continue;

            Object key = getKey.invoke(material);
            String id = key.toString().toLowerCase(Locale.ROOT);
            if (!id.startsWith("minecraft:")) continue;

            boolean block = (Boolean) isBlock.invoke(material);
            boolean solid = block && isSolid != null && (Boolean) isSolid.invoke(material);
            result.add(new Entry(id, block, solid));
        }

        result.sort(Comparator.comparing(Entry::id));
        return result;
    }
}
