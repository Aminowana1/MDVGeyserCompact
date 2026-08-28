# MDVGeyserCompat 1.0.0

Extension server-side de Geyser para MDVCRAFT. No requiere Fabric/Forge ni mods del cliente.

## Que hace

### 1. `minecraft:item_model` vanilla en items base

Registra todos los materiales vanilla que Bukkit/Purpur 1.21.6 expone como items para cada `base-items` configurado.

Por defecto:

```yaml
item-models:
  base-items:
    - minecraft:stick
    - minecraft:apple
```

Ejemplos Java que la extension intenta representar tambien en Bedrock:

```mcfunction
/give @s minecraft:stick[minecraft:item_model="minecraft:diamond_pickaxe"]
/give @s minecraft:stick[minecraft:item_model="minecraft:chorus_fruit"]
/give @s minecraft:apple[minecraft:item_model="minecraft:diamond_pickaxe"]
```

Un STICK normal sigue siendo STICK. La definicion solo coincide cuando el stack tiene el `item_model` correspondiente.

La extension genera automaticamente `MDVGeyserCompat-AutoPack.mcpack`. No incluye PNGs de Minecraft: solamente crea aliases hacia recursos vanilla que el cliente Bedrock ya posee.

Los bloques pueden usar render 3D mediante `use-3d-block-icons: true`. Geyser implementa esto con `block_placer`; Bedrock puede hacer una prediccion visual de colocacion al usar el item. Si molesta para una skill, ponlo en `false`.

Para nombres de textura Bedrock excepcionales puede añadirse:

```yaml
item-models:
  texture-overrides:
    - minecraft:ejemplo=textures/items/ruta_bedrock
```

### 2. Custom PLAYER_HEAD Base64

Busca perfiles Base64 de `textures.minecraft.net` en:

- YAML/JSON/TXT/CONF/properties dentro de `plugins/`
- clases y configs empaquetadas dentro de JARs (si `scan-jars: true`)
- `manual-profiles` del config

Luego los registra con `GeyserDefineCustomSkullsEvent` como `PROFILE`.

Esto utiliza el sistema oficial de custom skulls de Geyser para que puedan verse en Bedrock como:

- items en inventarios y menus
- cabezas equipadas por jugadores
- cabezas equipadas por mobs/entidades
- cabezas colocadas, cuando Geyser pueda asociar el perfil registrado

El listado detectado se guarda en `skulls-found.txt` dentro de la carpeta de la extension.

## Instalacion

1. Compila con Java 21: `mvn clean package`.
2. Copia `target/MDVGeyserCompat-1.0.0.jar` a:

   `plugins/Geyser-Spigot/extensions/MDVGeyserCompat-1.0.0.jar`

3. En `plugins/Geyser-Spigot/config.yml` deja:

```yaml
gameplay:
  enable-custom-content: true
```

4. Reinicia COMPLETAMENTE el servidor.
5. La config de esta extension quedara bajo la carpeta de datos que Geyser cree para `MDVGeyserCompat`.

## Compilar con GitHub Actions

El proyecto incluye `.github/workflows/build.yml`. Sube el proyecto a GitHub y ejecuta **Build MDVGeyserCompat** desde Actions; el JAR aparecera en el artifact `MDVGeyserCompat`.

## Notas

- Los custom items/skulls se definen durante el arranque de Geyser, por eso no se pueden descubrir y añadir de forma fiable despues de que ya entro un cliente Bedrock.
- Anadir muchas `base-items` multiplica las definiciones. Para MDVCRAFT conviene dejar STICK y solo agregar otras bases que realmente uses.
- `minecraft:item_model` y el antiguo `minecraft:custom_model_data` numerico no son exactamente lo mismo. Esta version esta enfocada en el componente moderno `minecraft:item_model` usado en 1.21.6.
