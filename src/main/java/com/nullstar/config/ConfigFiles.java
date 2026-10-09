package com.nullstar.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

public final class ConfigFiles {
    public static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private ConfigFiles() {}
    public static <T> T load(Path path, Class<T> type, Supplier<T> defaults) throws IOException {
        Files.createDirectories(path.getParent());
        if (Files.exists(path)) {
            try (var reader = Files.newBufferedReader(path)) {
                T value = JSON.fromJson(reader, type);
                if (value != null) return value;
                throw new IOException("Empty config: " + path);
            }
        }
        T value = defaults.get();
        Files.writeString(path, JSON.toJson(value)); return value;
    }
}
