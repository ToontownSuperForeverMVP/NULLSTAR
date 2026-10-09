package com.nullstar.core;

import com.nullstar.config.ConfigFiles;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Bundled defaults, with validated operator overrides in config/nullstar/weapons/<id>.json. */
public final class WeaponCatalog {
    public static final List<String> IDS = List.of("helios", "singularity", "glassfall");
    private final Map<String, WeaponDefinition> definitions = new LinkedHashMap<>();
    public WeaponCatalog(Path overrides) throws IOException {
        this(overrides, 1);
    }
    public WeaponCatalog(Path overrides, double durationMultiplier) throws IOException {
        if (Files.isDirectory(overrides)) {
            try (var files = Files.list(overrides)) {
                files.filter(Files::isRegularFile).filter(path -> !IDS.stream().anyMatch(id -> path.getFileName().toString().equals(id + ".json")))
                        .forEach(path -> com.nullstar.Nullstar.LOGGER.warn("Ignoring unknown weapon override: {}", path));
            }
        }
        for (String id : IDS) {
            WeaponDefinition definition;
            Path file = overrides.resolve(id + ".json");
            if (Files.exists(file)) {
                try (var reader = Files.newBufferedReader(file)) { definition = ConfigFiles.JSON.fromJson(reader, WeaponDefinition.class); }
            } else {
                try (var stream = WeaponCatalog.class.getResourceAsStream("/data/nullstar/weapons/" + id + ".json")) {
                    if (stream == null) throw new IOException("Missing bundled weapon " + id);
                    definition = ConfigFiles.JSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), WeaponDefinition.class);
                }
            }
            if (definition == null || !definition.id().equals(id) || !definition.behavior().equals(id))
                throw new IOException("Weapon id/behavior mismatch for " + id);
            definitions.put(id, definition.scaled(durationMultiplier));
        }
    }
    public WeaponDefinition get(String id) {
        WeaponDefinition definition = definitions.get(id);
        if (definition == null) throw new IllegalArgumentException("Unknown weapon: " + id);
        return definition;
    }
    public WeaponDefinition next(String id) { return get(IDS.get((Math.max(0, IDS.indexOf(id)) + 1) % IDS.size())); }
    public java.util.Collection<WeaponDefinition> all() { return java.util.Collections.unmodifiableCollection(definitions.values()); }
}
