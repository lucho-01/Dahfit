package com.dafit.infrastructure.config;

import com.dafit.infrastructure.adapter.out.persistence.DatabaseConnectivityCheck;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Ejecuta la verificación de base de datos al arrancar.
 *
 * <p>Separado de {@link DatabaseConnectivityCheck} para que el chequeo sea
 * reutilizable (health, tests) sin dispararse solo por crearse el bean.
 */
@Component
@ConditionalOnProperty(name = "dafit.database.verify-on-startup", havingValue = "true", matchIfMissing = false)
public class DatabaseStartupVerifier {

    private final DatabaseConnectivityCheck check;

    public DatabaseStartupVerifier(DatabaseConnectivityCheck check) {
        this.check = check;
    }

    @PostConstruct
    void verify() {
        check.verify();
    }
}
