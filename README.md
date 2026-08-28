# MDVGeyserCompat 1.0.4

Extension server-side de Geyser para MDVCRAFT (Purpur/Spigot + Geyser 2.11.x).

## 1. `minecraft:item_model` vanilla -> Bedrock

La 1.0.4 mantiene el escaneo exacto de MMOItems desde:

```text
plugins/MMOItems/item/
```

y registra solamente los pares reales `material -> model` de tus YAML.

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

### Cambio importante de 1.0.4

La 1.0.3 intentaba resolver muchos bloques mediante rutas de textura y ademas agregaba un predicate `count(1)` a todas las definiciones.

En 1.0.4:

- cada combinacion `base item + item_model` se registra SIN predicate adicional;
- los modelos que apuntan a bloques vanilla usan el icono/render nativo del bloque Bedrock mediante `block_placer + useBlockIcon`;
- `kelp` y `chain` reutilizan directamente los shortnames del atlas vanilla de Bedrock;
- `scute` / `turtle_scute` usa el shortname historico `turtle_shell_piece` de Bedrock;
- el AutoPack cambia de UUID para obligar a Bedrock a descargar esta revision y no reutilizar una copia antigua en cache.

Esto apunta directamente a los casos de MDVCRAFT que seguian fallando:

- STICK -> KELP
- STONE_PICKAXE -> BAMBOO
- STICK -> MOSS_BLOCK
- STICK -> LIGHTNING_ROD
- STICK -> CHAIN
- STICK -> COAL_BLOCK / OBSIDIAN
- STICK -> OCHRE/PEARLESCENT/VERDANT_FROGLIGHT
- STICK -> REDSTONE_BLOCK
- STICK -> SCUTE

Los bloques ya no dependen de adivinar una PNG del atlas de Bedrock para su icono: Bedrock renderiza su bloque vanilla.

### Reportes

Se generan:

```text
item-model-pairs-report.txt
item-texture-report.txt
item-model-failures-report.txt
item-model-registration-report.txt
```

`item-model-registration-report.txt` indica para cada par uno de estos modos:

```text
NATIVE_BLOCK
VANILLA_ATLAS
GENERATED_TEXTURE
```

y tambien indica `FAIL` si Geyser rechazo el registro.

## 2. Custom skulls Base64 -> Bedrock

No se cambia el sistema de cabezas de 1.0.3. Sigue usando `skulls-cache.txt` y registra los perfiles Base64 en Geyser.

## Instalacion

Es una extension de Geyser, no un plugin Bukkit normal:

```text
plugins/Geyser-Spigot/extensions/MDVGeyserCompat-1.0.4.jar
```

Borra la version anterior y reinicia el servidor completo.

Geyser debe tener:

```yaml
gameplay:
  enable-custom-content: true
```

## Compilar

Java 21 + Maven:

```bash
mvn clean package
```

Salida:

```text
target/MDVGeyserCompat-1.0.4.jar
```

El repositorio incluye `.github/workflows/build.yml` para compilarlo con GitHub Actions.
