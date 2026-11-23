package com.dafit.infrastructure.adapter.out.persistence;

import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/** Comprueba que la conexion JDBC del adaptador de persistencia esta disponible. */
@Component
public class JdbcDatabaseConnectivityCheck implements DatabaseConnectivityCheck {

    private static final int VALIDATION_TIMEOUT_SECONDS = 5;

    private final DataSource dataSource;

    public JdbcDatabaseConnectivityCheck(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void verify() {
        try (Connection connection = dataSource.getConnection()) {
            if (!connection.isValid(VALIDATION_TIMEOUT_SECONDS)) {
                throw new IllegalStateException(
                        "La verificacion de conectividad detecto una conexion no valida");
            }
        } catch (SQLException ex) {
            throw new IllegalStateException(
                    "No se pudo verificar la conectividad de la base de datos", ex);
        }
    }
}
