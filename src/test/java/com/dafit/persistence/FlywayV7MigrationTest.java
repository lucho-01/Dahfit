package com.dafit.persistence;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayV7MigrationTest {

    @Test
    void convertsExistingPercentilesAndCreatesSyncHistoryTables() throws Exception {
        String url = "jdbc:h2:mem:dafit-v7-" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE benchmark_points (
                        percentile INTEGER NOT NULL CHECK (percentile BETWEEN 0 AND 100)
                    )
                    """);
            statement.execute("INSERT INTO benchmark_points (percentile) VALUES (50)");
        }

        Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion(MigrationVersion.fromVersion("6"))
                .load()
                .migrate();

        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement();
             var result = statement.executeQuery(
                     "SELECT percentile FROM benchmark_points")) {
            assertThat(result.next()).isTrue();
            assertThat(result.getBigDecimal("percentile"))
                    .isEqualByComparingTo("50.00");
        }
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement();
             var result = statement.executeQuery("""
                     SELECT numeric_precision, numeric_scale
                     FROM information_schema.columns
                     WHERE table_name = 'BENCHMARK_POINTS'
                       AND column_name = 'PERCENTILE'
                     """)) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt("numeric_precision")).isEqualTo(5);
            assertThat(result.getInt("numeric_scale")).isEqualTo(2);
        }
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement();
             var result = statement.executeQuery("""
                     SELECT COUNT(*) FROM information_schema.tables
                     WHERE table_name IN ('SYNC_BATCHES', 'SYNC_BATCH_RESULTS')
                     """)) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isEqualTo(2);
        }
    }
}
