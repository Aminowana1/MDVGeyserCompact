package com.mdvcraft.geysercompat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

final class VanillaMaterialRegistry {
    record Entry(String id, boolean block) {}

    private VanillaMaterialRegistry() {}

    static List<Entry> discover() throws ReflectiveOperationException {
        Class<?> materialClass = Class.forName("org.bukkit.Material");
        Object[] values = (Object[]) materialClass.getMethod("values").invoke(null);
        Method isItem = materialClass.getMethod("isItem");
        Method isBlock = materialClass.getMethod("isBlock");
        Method getKey = materialClass.getMethod("getKey");

        List<Entry> result = new ArrayList<>();
        for (Object material : values) {
            if (!(Boolean) isItem.invoke(material)) continue;
            String enumName = ((Enum<?>) material).name();
            if (enumName.startsWith("LEGACY_") || enumName.equals("AIR") || enumName.endsWith("_AIR")) continue;

            Object key = getKey.invoke(material);
            String id = key.toString().toLowerCase(Locale.ROOT);
            if (!id.startsWith("minecraft:")) continue;
            result.add(new Entry(id, (Boolean) isBlock.invoke(material)));
        }

        result.sort(Comparator.comparing(Entry::id));
        return result;
    }
}
