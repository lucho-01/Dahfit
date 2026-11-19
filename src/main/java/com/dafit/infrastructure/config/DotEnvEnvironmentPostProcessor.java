package com.dafit.infrastructure.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Carga {@code .env} como fuente de propiedades de Spring.
 *
 * <p>Se ejecuta antes de que se cree el contexto, así que
 * {@code ${DATABASE_URL}} en {@code application.yml} ya está resuelto. No se
 * usa una librería externa: son 20 líneas y no hay dependencia adicional.
 *
 * <p>Precedencia (de mayor a menor): variables de entorno del sistema / línea
 * de comandos &gt; {@code .env} &gt; {@code application.yml}. Es decir, en
 * producción (Neon, contenedores) las variables ya inyectadas ganan y el
 * archivo se ignora o ni siquiera existe.
 *
 * <p>Si el archivo no existe se continúa en silencio: es el caso normal en
 * tests y en despliegues con variables de entorno.
 */
public class DotEnvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    static final String FILE_NAME = ".env";
    static final String SOURCE_NAME = "dotenv";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path file = locate();
        if (file == null) {
            return;
        }

        Map<String, Object> values = read(file);
        if (values.isEmpty()) {
            return;
        }

        // addLast: cualquier variable de entorno real ya presente tiene prioridad.
        environment.getPropertySources().addLast(new MapPropertySource(SOURCE_NAME, values));
    }

    private Path locate() {
        List<Path> candidates = List.of(
                Paths.get(FILE_NAME).toAbsolutePath(),
                Paths.get(System.getProperty("user.dir"), FILE_NAME).toAbsolutePath());
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private Map<String, Object> read(Path file) {
        Map<String, Object> values = new LinkedHashMap<>();
        try {
            for (String rawLine : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String line = rawLine.strip();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int separator = line.indexOf('=');
                if (separator <= 0) {
                    continue;
                }
                String key = line.substring(0, separator).strip();
                String value = stripQuotes(line.substring(separator + 1).strip());
                if (!key.isEmpty() && !value.isEmpty()) {
                    values.put(key, value);
                }
            }
        } catch (IOException ex) {
            // Un .env ilegible no debe tumbar el arranque: las variables de
            // entorno siguen disponibles.
            return Map.of();
        }
        return values;
    }

    private String stripQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
