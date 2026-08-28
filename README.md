# MDVGeyserCompat 1.0.5

Extension server-side de Geyser para MDVCRAFT (Purpur/Spigot + Geyser 2.11.x).

## Cambio critico de 1.0.5

La 1.0.4 quitaba el predicate de las definiciones `minecraft:*`. Eso era incorrecto: Geyser exige un predicate cuando el `item_model` esta en el namespace `minecraft`.

La 1.0.5 vuelve a usar `ItemRangeDispatchPredicate.count(1)` para todos los modelos vanilla y elimina el uso de `block_placer` como truco visual. `block_placer` esta pensado para items que colocan bloques y, con `useBlockIcon=true`, Geyser suprime el `minecraft:icon` del custom item.

Ahora todos los pares reales leidos desde:

```text
plugins/MMOItems/item/
```

se registran como:

```text
material Java + minecraft:item_model -> custom item Bedrock con icono explicito
```

Incluye fallbacks internos para los casos MDVCRAFT reportados:

```text
STICK -> KELP
STONE_PICKAXE -> BAMBOO
STICK -> MOSS_BLOCK
STICK -> LIGHTNING_ROD
STICK -> CHAIN
STICK -> COAL_BLOCK
STICK -> OBSIDIAN
STICK -> OCHRE_FROGLIGHT
STICK -> PEARLESCENT_FROGLIGHT
STICK -> VERDANT_FROGLIGHT
STICK -> REDSTONE_BLOCK
STICK -> SCUTE
```

`SCUTE` conserva `turtle_shell_piece`, que ya funcionaba en Bedrock.

Para bloques se usa una textura de item explicita en el AutoPack en vez de hacer que el custom item finja ser un bloque colocable. Esto prioriza que el item se vea correctamente en inventario, menus, mano y drops.

## Reportes

Se generan:

```text
item-model-pairs-report.txt
item-texture-report.txt
item-model-failures-report.txt
item-model-registration-report.txt
```

Los modos de registro de 1.0.5 son:

```text
VANILLA_ATLAS
EXPLICIT_TEXTURE
```

## Custom skulls Base64

El sistema de skulls no cambia. Sigue usando cache y registra los perfiles Base64 en Geyser.

## Instalacion

Es una extension de Geyser:

```text
plugins/Geyser-Spigot/extensions/MDVGeyserCompat-1.0.5.jar
```

Elimina la version anterior y reinicia completamente el servidor.

## Compilar

Java 21 + Maven:

```bash
mvn clean package
```

Salida:

```text
target/MDVGeyserCompat-1.0.5.jar
```

Incluye `.github/workflows/build.yml` para GitHub Actions.
