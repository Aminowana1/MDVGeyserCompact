package com.mdvcraft.geysercompat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detecta pares reales de item base + minecraft:item_model usados por configs.
 *
 * Objetivo: no obligar a meter TRIDENT, PAPER, STONE_PICKAXE, comida, etc. en
 * base-items manualmente. Tambien evita registrar 1.400 modelos para cientos de
 * materiales base que nunca se usan.
 */
final class ItemModelPairScanner {
    private static final Pattern INLINE_STACK = Pattern.compile(
            "(?i)(?:minecraft:)?([a-z0-9_]+)\\s*\\[[^\\]]{0,500}?item_model\\s*=\\s*[\\\"']?(?:minecraft:)?([a-z0-9_]+)");
    private static final Pattern INLINE_JSON_MODEL = Pattern.compile(
            "(?i)\\\"?item_model\\\"?\\s*[:=]\\s*[\\\"'](?:minecraft:)?([a-z0-9_]+)[\\\"']");

    private static final Set<String> BASE_KEYS = Set.of(
            "material", "id", "item", "item-material", "item_material", "itemmaterial");
    private static final Set<String> MODEL_KEYS = Set.of(
            "model", "item-model", "item_model", "itemmodel", "item-model-id", "item_model_id");

    record Result(Map<String, Set<String>> pairs, int filesScanned, long millis) {
        int pairCount() {
            int total = 0;
            for (Set<String> targets : pairs.values()) total += targets.size();
            return total;
        }
    }

    private record Scope(int indent, String key, String path) {}

    private static final class ScopeData {
        String base;
        String model;
    }

    private ItemModelPairScanner() {}

    static Result scan(Path serverRoot,
                       CompatConfig config,
                       Set<String> validItems) {
        long started = System.currentTimeMillis();
        Map<String, Set<String>> pairs = new LinkedHashMap<>();
        int[] files = {0};
        long maxBytes = Math.max(1L, config.pairScanMaxFileSizeMb) * 1024L * 1024L;

        if (config.autoDetectPairs) {
            for (String rootText : config.pairScanRoots) {
                Path root = serverRoot.resolve(rootText).normalize();
                if (!root.startsWith(serverRoot) || Files.notExists(root)) continue;
                try {
                    Files.walkFileTree(root, new SimpleFileVisitor<>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            if (!attrs.isRegularFile() || attrs.size() > maxBytes) return FileVisitResult.CONTINUE;
                            String ext = extension(file.getFileName().toString());
                            if (!config.pairScanExtensions.contains(ext)) return FileVisitResult.CONTINUE;
                            // Evita escanear nuestra propia salida generada.
                            if (file.toString().contains("mdvgeysercompat")) return FileVisitResult.CONTINUE;
                            files[0]++;
                            scanFile(file, validItems, pairs);
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } catch (IOException ignored) {
                    // Un archivo/directorio bloqueado no debe impedir que Geyser arranque.
                }
            }
        }

        for (String manual : config.manualPairs) {
            addManual(manual, validItems, pairs);
        }

        // Congela orden estable para reportes reproducibles.
        Map<String, Set<String>> stable = new LinkedHashMap<>();
        pairs.keySet().stream().sorted().forEach(base -> {
            Set<String> sortedTargets = new LinkedHashSet<>();
            pairs.get(base).stream().sorted().forEach(sortedTargets::add);
            stable.put(base, sortedTargets);
        });

        return new Result(stable, files[0], System.currentTimeMillis() - started);
    }

    private static void scanFile(Path file,
                                 Set<String> validItems,
                                 Map<String, Set<String>> pairs) {
        final List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException ignored) {
            return;
        }

        // 1) Sintaxis tipo /give ...[minecraft:item_model="..."]
        for (String raw : lines) {
            Matcher inline = INLINE_STACK.matcher(raw);
            while (inline.find()) {
                String base = normalizeCandidate(inline.group(1));
                String target = normalizeCandidate(inline.group(2));
                addPair(base, target, validItems, pairs);
            }
        }

        // 2) YAML/config estilo MMOItems / MythicMobs:
        // ITEM:
        //   base:
        //     material: TRIDENT
        //     model: diamond_axe
        Deque<Scope> stack = new ArrayDeque<>();
        Map<String, ScopeData> scopes = new HashMap<>();

        for (String raw : lines) {
            String stripped = stripComment(raw);
            if (stripped.isBlank()) continue;
            int indent = leadingSpaces(stripped);
            String line = stripped.trim();

            while (!stack.isEmpty() && stack.peekLast().indent() >= indent) {
                stack.removeLast();
            }

            int colon = firstColonOutsideQuotes(line);
            if (colon <= 0) continue;
            String key = unquote(line.substring(0, colon).trim()).toLowerCase(Locale.ROOT);
            String value = line.substring(colon + 1).trim();

            String parentPath = stack.isEmpty() ? "" : stack.peekLast().path();
            String scopePath = parentPath;

            if (value.isBlank()) {
                String nodePath = parentPath.isEmpty() ? key : parentPath + "/" + key;
                stack.addLast(new Scope(indent, key, nodePath));
                continue;
            }

            ScopeData data = scopes.computeIfAbsent(scopePath, ignored -> new ScopeData());
            String scalar = cleanScalar(value);

            if (BASE_KEYS.contains(key)) {
                String candidate = normalizeCandidate(scalar);
                if (validItems.contains(candidate)) {
                    data.base = candidate;
                    if (data.model != null) addPair(data.base, data.model, validItems, pairs);
                }
            } else if (MODEL_KEYS.contains(key)) {
                String candidate = normalizeCandidate(scalar);
                if (validItems.contains(candidate)) {
                    data.model = candidate;
                    if (data.base != null) addPair(data.base, data.model, validItems, pairs);
                }
            } else {
                // JSON/configs en una sola linea donde aparece "item_model":"minecraft:x".
                Matcher json = INLINE_JSON_MODEL.matcher(line);
                if (json.find() && data.base != null) {
                    addPair(data.base, normalizeCandidate(json.group(1)), validItems, pairs);
                }
            }
        }

        // 3) Tolerancia para formatos donde material y model caen en sub-scope hermano.
        // Ejemplo raro:
        // material: STICK
        // options:
        //   model: DIAMOND_AXE
        // Solo busca una ventana corta y no cruza otro material valido.
        for (int i = 0; i < lines.size(); i++) {
            String stripped = stripComment(lines.get(i));
            if (stripped.isBlank()) continue;
            int colon = firstColonOutsideQuotes(stripped.trim());
            if (colon <= 0) continue;
            String key = unquote(stripped.trim().substring(0, colon).trim()).toLowerCase(Locale.ROOT);
            if (!BASE_KEYS.contains(key)) continue;

            String base = normalizeCandidate(cleanScalar(stripped.trim().substring(colon + 1).trim()));
            if (!validItems.contains(base)) continue;
            int baseIndent = leadingSpaces(stripped);

            int end = Math.min(lines.size(), i + 80);
            for (int j = i + 1; j < end; j++) {
                String candidateLine = stripComment(lines.get(j));
                if (candidateLine.isBlank()) continue;
                int candidateIndent = leadingSpaces(candidateLine);
                String trimmed = candidateLine.trim();
                int c = firstColonOutsideQuotes(trimmed);
                if (c <= 0) continue;
                String k = unquote(trimmed.substring(0, c).trim()).toLowerCase(Locale.ROOT);

                if (candidateIndent <= baseIndent && BASE_KEYS.contains(k)) break;
                if (MODEL_KEYS.contains(k)) {
                    String target = normalizeCandidate(cleanScalar(trimmed.substring(c + 1).trim()));
                    addPair(base, target, validItems, pairs);
                    break;
                }
            }
        }
    }

    private static void addManual(String raw,
                                  Set<String> validItems,
                                  Map<String, Set<String>> pairs) {
        if (raw == null) return;
        String value = raw.trim();
        int split = value.indexOf("=>");
        int width = 2;
        if (split < 0) {
            split = value.indexOf('=');
            width = 1;
        }
        if (split < 0) {
            split = value.indexOf("->");
            width = 2;
        }
        if (split <= 0 || split + width >= value.length()) return;
        addPair(normalizeCandidate(value.substring(0, split)),
                normalizeCandidate(value.substring(split + width)), validItems, pairs);
    }

    private static void addPair(String base,
                                String target,
                                Set<String> validItems,
                                Map<String, Set<String>> pairs) {
        if (base == null || target == null || base.equals(target)) return;
        if (!validItems.contains(base) || !validItems.contains(target)) return;
        pairs.computeIfAbsent(base, ignored -> new LinkedHashSet<>()).add(target);
    }

    private static String normalizeCandidate(String value) {
        if (value == null) return "";
        String v = cleanScalar(value).toLowerCase(Locale.ROOT);
        if (v.startsWith("minecraft:")) return v;
        if (!v.matches("[a-z0-9_./-]+")) return "";
        return "minecraft:" + v;
    }

    private static String cleanScalar(String value) {
        String v = value == null ? "" : value.trim();
        if (v.endsWith(",")) v = v.substring(0, v.length() - 1).trim();
        if (v.length() >= 2) {
            char first = v.charAt(0);
            char last = v.charAt(v.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                v = v.substring(1, v.length() - 1).trim();
            }
        }
        return v;
    }

    private static int firstColonOutsideQuotes(String value) {
        boolean single = false;
        boolean doub = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\'' && !doub) single = !single;
            else if (c == '"' && !single) doub = !doub;
            else if (c == ':' && !single && !doub) return i;
        }
        return -1;
    }

    private static String stripComment(String value) {
        boolean single = false;
        boolean doub = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\'' && !doub) single = !single;
            else if (c == '"' && !single) doub = !doub;
            else if (c == '#' && !single && !doub) return value.substring(0, i);
        }
        return value;
    }

    private static int leadingSpaces(String value) {
        int i = 0;
        while (i < value.length() && value.charAt(i) == ' ') i++;
        return i;
    }

    private static String unquote(String value) {
        String v = value.trim();
        if (v.length() >= 2) {
            char first = v.charAt(0);
            char last = v.charAt(v.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return v.substring(1, v.length() - 1);
            }
        }
        return v;
    }

    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) return "";
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
