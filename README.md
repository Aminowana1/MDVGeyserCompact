# MDVGeyserCompat 1.0.6

Extension server-side de Geyser para MDVCRAFT (Purpur/Spigot + Geyser 2.11.x).

## Cambio principal de 1.0.6

Los `item_model` cuyo target es un bloque vanilla ya no se dibujan con una PNG plana. Se registran con `GeyserBlockPlacer` + `useBlockIcon=true`, que es la ruta de Geyser para que Bedrock use el render 3D nativo del bloque como icono.

Esto apunta especialmente a:

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
```

Los targets que son items y no bloques, por ejemplo `SCUTE`, siguen usando el atlas/item texture normal de Bedrock.

Las definiciones `minecraft:*` conservan `ItemRangeDispatchPredicate.count(1)`, ya que Geyser exige al menos un predicate para modelos del namespace `minecraft`.

## Deteccion

Los pares reales se leen desde:

```text
plugins/MMOItems/item/
```

No importa en cual YAML esten.

## Config nueva

```yaml
item-models:
  native-block-rendering: true
```

La opcion viene activa por defecto incluso si conservas un `config.yml` viejo que todavia no tenga esa clave.

`block-id-overrides` sigue disponible por si un identificador Java y Bedrock no coincide:

```yaml
  block-id-overrides:
    # - minecraft:algo=minecraft:otro_id_bedrock
```

## Nota importante sobre Bedrock

El render 3D nativo de bloques se obtiene mediante el componente Bedrock `block_placer`. Geyser lo usa para que el cliente renderice el modelo 3D del bloque. El ItemStack real del servidor Java sigue siendo el material original, por ejemplo `STICK` o `STONE_PICKAXE`.

Si algun item con habilidad de click derecho muestra una prediccion visual rara al tocar bloques desde Bedrock, se puede desactivar globalmente con:

```yaml
item-models:
  native-block-rendering: false
```

y volver al fallback 2D.

## Reportes

```text
item-model-pairs-report.txt
item-texture-report.txt
item-model-failures-report.txt
item-model-registration-report.txt
```

Los modos de registro ahora pueden ser:

```text
NATIVE_BLOCK_3D(...)
VANILLA_ATLAS
EXPLICIT_TEXTURE
```

## Custom skulls Base64

No cambia. El sistema sigue usando cache y registra los perfiles Base64 en Geyser.

## Instalacion

Es una extension de Geyser:

```text
plugins/Geyser-Spigot/extensions/MDVGeyserCompat-1.0.6.jar
```

Elimina la version anterior y reinicia completamente el servidor.

## Compilar

Java 21 + Maven:

```bash
mvn clean package
```

Salida:

```text
target/MDVGeyserCompat-1.0.6.jar
```

Incluye `.github/workflows/build.yml` para GitHub Actions.
