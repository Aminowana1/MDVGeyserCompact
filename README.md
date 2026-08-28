# MDVGeyserCompat 1.0.3

Extension server-side de Geyser para MDVCRAFT (Purpur/Spigot + Geyser 2.11.x).

## 1. `minecraft:item_model` vanilla -> Bedrock

La 1.0.3 cambia la estrategia de registro para MDVCRAFT.

Todos los items con `item_model` se leen directamente desde:

```text
plugins/MMOItems/item/
```

y se registran solamente los pares reales `material -> model` de los YAML.

Ejemplos:

```yaml
AMBARSILVESTRE:
  base:
    material: STICK
    model: KELP
```

```yaml
OTROITEM:
  base:
    material: STONE_PICKAXE
    model: BAMBOO
```

Esto evita registrar `STICK x ~1400 modelos`, que en 1.0.2 producia miles de definiciones y hacia que Geyser omitiera muchas.

La config por defecto usa:

```yaml
item-models:
  mmoitems-only-mode: true
  mmoitems-folder: plugins/MMOItems/item
```

Incluso si conservas una config 1.0.2 con `base-items: STICK/APPLE`, el modo MMOItems de 1.0.3 los ignora para evitar volver al registro masivo.

### Casos vanilla corregidos

Se añadieron equivalencias explicitas Bedrock para:

- kelp
- bamboo
- moss_block
- lightning_rod
- chain
- coal_block
- obsidian
- redstone_block
- glowstone
- ochre/pearlescent/verdant_froglight
- scute / turtle_scute

Tambien se permite registrar un `item_model` aunque no corresponda exactamente a un `Material` Bukkit actual. Esto cubre nombres de modelo legacy como `minecraft:scute`.

### Reportes

Se generan:

```text
item-model-pairs-report.txt
item-texture-report.txt
item-model-failures-report.txt
```

`item-model-failures-report.txt` muestra exactamente cualquier definicion que Geyser haya rechazado.

Si un item se genera por codigo y no existe en `plugins/MMOItems/item`, puede agregarse manualmente:

```yaml
item-models:
  manual-pairs:
    - minecraft:paper=>minecraft:tripwire_hook
```

## 2. Custom skulls Base64 -> Bedrock

Se mantiene el sistema de 1.0.2: busca perfiles Base64, los registra en Geyser y usa `skulls-cache.txt` para que los reinicios sean rapidos.

Para reconstruir la cache:

```yaml
skulls:
  rebuild-cache: true
```

reinicia una vez y vuelve a `false`.

## Instalacion

Este JAR es una extension de Geyser, no un plugin Bukkit normal:

```text
plugins/Geyser-Spigot/extensions/MDVGeyserCompat-1.0.3.jar
```

Borra la version anterior y reinicia el servidor completo.

Geyser debe tener:

```yaml
enable-custom-content: true
```

## Compilar

Java 21 + Maven:

```bash
mvn clean package
```

Salida:

```text
target/MDVGeyserCompat-1.0.3.jar
```

El repositorio incluye `.github/workflows/build.yml` para compilarlo con GitHub Actions.
