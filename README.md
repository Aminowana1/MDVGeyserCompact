# MDVGeyserCompat 1.0.7

Geyser Extension para MDVCRAFT (Geyser 2.11.x / Java 21).

## Correccion 1.0.7

Geyser 2.11.x no permite `GeyserItemDataComponents.BLOCK_PLACER` en una `CustomItemDefinition` que extiende un item vanilla (por ejemplo STICK, PAPER o STONE_PICKAXE). La 1.0.6 intentaba usar ese componente para conseguir el icono 3D nativo de bloques y Geyser respondia:

```
IllegalArgumentException: That component cannot be used for vanilla items
```

La 1.0.7 elimina esa ruta y usa un fallback visual estable:

- atlas vanilla para items que lo tienen (`kelp`, `chain`, `scute`, etc.);
- `carried_texture` conocida cuando Bedrock la expone (`bamboo -> textures/items/bamboo`);
- textura de bloque valida para cubos como `moss_block`, `coal_block`, `redstone_block`, `obsidian` y froglights;
- aliases para `mangrove_roots`, `twisting_vines`, `jungle_sapling` y banners.

Esto prioriza que todos los `item_model` se vean y no queden omitidos/morados. Los bloques basados en un STICK no pueden usar el render 3D nativo de block-item con la API actual de Geyser; para eso Geyser tendria que permitir mapear el custom item a un block item/custom block.

## Instalacion

1. Compilar con GitHub Actions o Maven Java 21.
2. Poner el JAR en `plugins/Geyser-Spigot/extensions/`.
3. Dejar una sola version de MDVGeyserCompat.
4. Reiniciar completamente el servidor.

La extension genera un UUID nuevo del AutoPack 1.0.7 para forzar que Bedrock no reutilice el pack anterior.
