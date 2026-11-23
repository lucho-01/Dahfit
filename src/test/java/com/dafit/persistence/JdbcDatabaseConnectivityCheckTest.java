package com.dafit.persistence;

import com.dafit.infrastructure.adapter.out.persistence.JdbcDatabaseConnectivityCheck;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JdbcDatabaseConnectivityCheckTest {

    @Test
    void verifiesTheConnectionReturnedByTheDataSource() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenReturn(true);

        new JdbcDatabaseConnectivityCheck(dataSource).verify();

        verify(connection).isValid(5);
        verify(connection).close();
    }

    @Test
    void failsClearlyWhenTheConnectionIsInvalid() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenReturn(false);

        assertThatThrownBy(() -> new JdbcDatabaseConnectivityCheck(dataSource).verify())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("conectividad");

        verify(connection).close();
    }

    @Test
    void preservesTheCauseWhenTheDatasourceCannotConnect() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        SQLException failure = new SQLException("conexion no disponible");
        when(dataSource.getConnection()).thenThrow(failure);

        assertThatThrownBy(() -> new JdbcDatabaseConnectivityCheck(dataSource).verify())
                .isInstanceOf(IllegalStateException.class)
                .hasCause(failure);
    }
}
