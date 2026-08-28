# MDVGeyserCompat 1.0.1

Extension server-side de Geyser para MDVCRAFT (Purpur/Spigot + Geyser 2.11.x).

## Funciones

### 1. `minecraft:item_model` vanilla -> Bedrock
Registra, para cada material base configurado (por defecto `minecraft:stick` y `minecraft:apple`), apariencias para todos los materiales vanilla detectados en la version Bukkit del servidor.

Ejemplos Java:

```mcfunction
/give @s minecraft:stick[minecraft:item_model="minecraft:diamond_pickaxe"]
/give @s minecraft:stick[minecraft:item_model="minecraft:chorus_fruit"]
/give @s minecraft:apple[minecraft:item_model="minecraft:diamond_pickaxe"]
```

La extension crea automaticamente `MDVGeyserCompat-AutoPack.mcpack` y lo registra en Geyser.

### Cambios 1.0.1
- Todos los mappings, incluidos los que apuntan a bloques, reciben ahora un icono de inventario. Esto corrige los modelos que se veian bien en mano/dropeados pero fallaban en menus/GUI.
- Se ampliaron las traducciones Java -> rutas vanilla Bedrock (salmon, libros, pociones, tintes, puertas, carteles, botes, discos, etc.).
- Se genera `item-texture-report.txt` para diagnosticar rapidamente cualquier excepcion restante.
- Se filtran materiales `LEGACY_*` antes de consultar Bukkit, evitando inicializar soporte legacy solo por el escaneo.

### 2. Custom skulls Base64 -> Bedrock
Busca perfiles Base64 de `textures.minecraft.net` en configuraciones y opcionalmente dentro de JARs, y los registra con la API de custom skulls de Geyser. Funcionan en inventarios/menus, equipados por jugadores y mobs y como items.

Desde 1.0.1 los perfiles se cachean en `skulls-cache.txt`. Si ya venias de 1.0.0, reutiliza automaticamente `skulls-found.txt` para evitar el escaneo completo de nuevo.

Para detectar cabezas nuevas, pon temporalmente:

```yaml
skulls:
  rebuild-cache: true
```

reinicia una vez y luego vuelve a `false`.

## Instalacion

**No es un plugin Bukkit normal.** Copia el JAR a:

```text
plugins/Geyser-Spigot/extensions/MDVGeyserCompat-1.0.1.jar
```

Asegurate de tener en Geyser:

```yaml
enable-custom-content: true
```

Reinicia completamente el servidor. No uses `/reload`.

## Compilar con Maven

Requiere Java 21 y Maven:

```bash
mvn clean package
```

Salida:

```text
target/MDVGeyserCompat-1.0.1.jar
```

El workflow `.github/workflows/build.yml` tambien permite compilarlo desde GitHub Actions.
