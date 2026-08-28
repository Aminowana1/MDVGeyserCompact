package com.mdvcraft.geysercompat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SkullScanner {
    // Los perfiles texture de Mojang suelen rondar 180-500 caracteres.
    // Se usa un mínimo alto para evitar confundir UUIDs/tokens con perfiles.
    private static final Pattern BASE64 = Pattern.compile("(?<![A-Za-z0-9+/])([A-Za-z0-9+/]{100,}={0,2})(?![A-Za-z0-9+/=])");
    private static final String CACHE_FILE = "skulls-cache.txt";
    private static final String LEGACY_CACHE_FILE = "skulls-found.txt";

    record Result(Set<String> profiles, int filesScanned, int jarsScanned, long millis, boolean fromCache) {}

    private SkullScanner() {}

    static Result loadOrScan(Path serverRoot, Path dataFolder, CompatConfig cfg) {
        long start = System.currentTimeMillis();
        Set<String> manual = new HashSet<>();
        for (String profile : cfg.manualProfiles) {
            if (isTextureProfile(profile)) manual.add(profile);
        }

        if (cfg.skullCacheEnabled && !cfg.rebuildSkullCache) {
            Path cache = dataFolder.resolve(CACHE_FILE);
            Set<String> cached = readCache(cache);

            // Migración automática desde 1.0.0: reaprovecha skulls-found.txt y evita
            // volver a escanear centenares de JARs en el primer arranque con 1.0.1.
            if (cached.isEmpty() && Files.notExists(cache)) {
                Path legacy = dataFolder.resolve(LEGACY_CACHE_FILE);
                cached = readCache(legacy);
                if (!cached.isEmpty()) {
                    cached.addAll(manual);
                    writeCache(cache, cached);
                }
            }

            if (!cached.isEmpty()) {
                cached.addAll(manual);
                return new Result(cached, 0, 0, System.currentTimeMillis() - start, true);
            }
        }

        Result scanned = scan(serverRoot, cfg);
        Set<String> profiles = new HashSet<>(scanned.profiles());
        profiles.addAll(manual);

        if (cfg.skullCacheEnabled) {
            writeCache(dataFolder.resolve(CACHE_FILE), profiles);
        }
        // Se mantiene por compatibilidad y para que el administrador pueda inspeccionarlo.
        writeCache(dataFolder.resolve(LEGACY_CACHE_FILE), profiles);

        return new Result(profiles, scanned.filesScanned(), scanned.jarsScanned(),
                System.currentTimeMillis() - start, false);
    }

    private static Result scan(Path serverRoot, CompatConfig cfg) {
        long start = System.currentTimeMillis();
        Set<String> profiles = new HashSet<>();
        profiles.addAll(cfg.manualProfiles.stream().filter(SkullScanner::isTextureProfile).toList());

        AtomicInteger files = new AtomicInteger();
        AtomicInteger jars = new AtomicInteger();
        long maxFileBytes = Math.max(1, cfg.maxFileSizeMb) * 1024L * 1024L;
        long maxJarEntryBytes = Math.max(1, cfg.maxJarEntryMb) * 1024L * 1024L;

        if (cfg.scanServerFiles) {
            for (String configuredRoot : cfg.scanRoots) {
                Path root = serverRoot.resolve(configuredRoot).normalize();
                if (!root.startsWith(serverRoot) || !Files.exists(root)) continue;

                try (var stream = Files.walk(root)) {
                    stream.filter(Files::isRegularFile).forEach(path -> {
                        try {
                            long size = Files.size(path);
                            String ext = extension(path);

                            if (cfg.textExtensions.contains(ext) && size <= maxFileBytes) {
                                scanBytes(Files.readAllBytes(path), profiles);
                                files.incrementAndGet();
                            } else if (cfg.scanJars && ext.equals("jar")) {
                                scanJar(path, profiles, maxJarEntryBytes);
                                jars.incrementAndGet();
                            }
                        } catch (Exception ignored) {
                            // Un archivo ilegible no debe impedir iniciar Geyser.
                        }
                    });
                } catch (IOException ignored) {
                    // Rutas opcionales pueden no ser accesibles.
                }
            }
        }

        return new Result(profiles, files.get(), jars.get(), System.currentTimeMillis() - start, false);
    }

    private static Set<String> readCache(Path path) {
        Set<String> result = new HashSet<>();
        if (Files.notExists(path)) return result;
        try {
            for (String raw : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                String value = raw.trim();
                if (isTextureProfile(value)) result.add(value);
            }
        } catch (IOException ignored) {
        }
        return result;
    }

    private static void writeCache(Path path, Set<String> profiles) {
        try {
            Files.createDirectories(path.getParent());
            Path temp = path.resolveSibling(path.getFileName() + ".tmp");
            Files.write(temp, profiles.stream().sorted().toList(), StandardCharsets.UTF_8);
            try {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicUnsupported) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) {
        }
    }

    private static void scanJar(Path jarPath, Set<String> profiles, long maxEntryBytes) {
        try (JarFile jar = new JarFile(jarPath.toFile(), false)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;
                long declared = entry.getSize();
                if (declared > maxEntryBytes) continue;

                String name = entry.getName().toLowerCase(Locale.ROOT);
                if (!(name.endsWith(".class") || name.endsWith(".yml") || name.endsWith(".yaml")
                        || name.endsWith(".json") || name.endsWith(".txt") || name.endsWith(".conf")
                        || name.endsWith(".properties"))) {
                    continue;
                }

                try (InputStream in = jar.getInputStream(entry)) {
                    byte[] data = readLimited(in, maxEntryBytes);
                    if (data != null) scanBytes(data, profiles);
                } catch (Exception ignored) {
                }
            }
        } catch (IOException ignored) {
        }
    }

    private static byte[] readLimited(InputStream in, long maxBytes) throws IOException {
        byte[] buffer = new byte[8192];
        List<byte[]> chunks = new ArrayList<>();
        int total = 0;
        int read;
        while ((read = in.read(buffer)) != -1) {
            total += read;
            if (total > maxBytes) return null;
            byte[] chunk = new byte[read];
            System.arraycopy(buffer, 0, chunk, 0, read);
            chunks.add(chunk);
        }
        byte[] out = new byte[total];
        int offset = 0;
        for (byte[] chunk : chunks) {
            System.arraycopy(chunk, 0, out, offset, chunk.length);
            offset += chunk.length;
        }
        return out;
    }

    private static void scanBytes(byte[] data, Set<String> profiles) {
        // ISO-8859-1 conserva 1 byte -> 1 char y permite encontrar strings en .class.
        String text = new String(data, StandardCharsets.ISO_8859_1);
        Matcher matcher = BASE64.matcher(text);
        while (matcher.find()) {
            String candidate = matcher.group(1);
            if (isTextureProfile(candidate)) profiles.add(candidate);
        }
    }

    static boolean isTextureProfile(String candidate) {
        if (candidate == null || candidate.length() < 100) return false;
        try {
            byte[] decoded = Base64.getDecoder().decode(candidate);
            String json = new String(decoded, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
            return json.contains("textures")
                    && json.contains("skin")
                    && (json.contains("textures.minecraft.net") || json.contains("\"url\""));
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static String extension(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot == -1 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
