# MDVGeyserCompat 1.0.2

Extension server-side de Geyser para MDVCRAFT (Purpur/Spigot + Geyser 2.11.x).

## 1. `minecraft:item_model` vanilla -> Bedrock

Replica en Bedrock los items Java que cambian visualmente mediante `minecraft:item_model`.

### Bases amplias

Por defecto `STICK` y `APPLE` pueden verse como cualquier item/bloque vanilla:

```mcfunction
/give @s minecraft:stick[minecraft:item_model="minecraft:diamond_pickaxe"]
/give @s minecraft:stick[minecraft:item_model="minecraft:sunflower"]
/give @s minecraft:apple[minecraft:item_model="minecraft:chorus_fruit"]
```

### Deteccion automatica de pares (nuevo 1.0.2)

La 1.0.1 solo registraba las bases listadas en `base-items`. Por eso un item cuya base real era
`TRIDENT`, `STONE_PICKAXE`, `PAPER`, `SALMON`, etc. se quedaba con la apariencia de su item base.

La 1.0.2 escanea configs YAML/JSON/TXT y registra solo los pares usados, por ejemplo:

```yaml
material: TRIDENT
model: diamond_axe
```

```yaml
material: STONE_PICKAXE
model: bamboo
```

```yaml
material: PAPER
model: tripwire_hook
```

No se registra la matriz TODOS x TODOS (serian millones de definiciones); las bases amplias cubren
los comodines y el escaner agrega los pares reales del servidor.

Si un item se crea exclusivamente por codigo y no aparece en ninguna config, se puede agregar:

```yaml
item-models:
  manual-pairs:
    - minecraft:stone_pickaxe=>minecraft:feather
```

Se genera `item-model-pairs-report.txt` para ver los pares detectados.

### Plantas y bloques

Desde 1.0.2 solo los bloques solidos usan `block_placer`/render 3D de Bedrock. Plantas, flores,
bambu, tripwire hook y otros bloques no solidos usan sus texturas carried/planas. Esto evita
missing textures morado/negro y modelos incorrectos en inventarios/menus.

Se ampliaron equivalencias vanilla para sunflower, bamboo, tripwire hook, flores dobles, flores
normales, pitcher plant, torchflower, pink petals, wildflowers, firefly bush y otros items modernos.

La extension genera automaticamente `MDVGeyserCompat-AutoPack.mcpack` y
`item-texture-report.txt`.

## 2. Custom skulls Base64 -> Bedrock

Busca perfiles Base64 de `textures.minecraft.net`, los registra en Geyser y los cachea en
`skulls-cache.txt`. Funcionan en menus/inventarios, en mano, equipados por jugadores y mobs.

Para reconstruir la cache de cabezas:

```yaml
skulls:
  rebuild-cache: true
```

reinicia una vez y vuelve a `false`.

## Instalacion

No es un plugin Bukkit normal. Copia el JAR a:

```text
plugins/Geyser-Spigot/extensions/MDVGeyserCompat-1.0.2.jar
```

Borra la version anterior para no cargar dos extensiones con el mismo ID y haz un reinicio completo.

En Geyser debe estar:

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
target/MDVGeyserCompat-1.0.2.jar
```
