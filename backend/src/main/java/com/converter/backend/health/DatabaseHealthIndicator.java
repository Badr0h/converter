package com.converter.backend.health;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    @Autowired
    public DatabaseHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection != null && !connection.isClosed()) {
                // Test a simple query to ensure database is responsive
                boolean isValid = connection.isValid(2); // 2 second timeout
                
                if (isValid) {
                    return Health.up()
                            .withDetail("database", "PostgreSQL")
                            .withDetail("status", "Available")
                            .withDetail("validation", "Connection successful")
                            .build();
                } else {
                    return Health.down()
                            .withDetail("database", "PostgreSQL")
                            .withDetail("status", "Unavailable")
                            .withDetail("error", "Connection validation failed")
                            .build();
                }
            }
        } catch (SQLException e) {
            return Health.down()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("status", "Error")
                    .withDetail("error", e.getMessage())
                    .withDetail("error_code", e.getErrorCode())
                    .withDetail("sql_state", e.getSQLState())
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("status", "Error")
                    .withDetail("error", e.getMessage())
                    .build();
        }

        return Health.down()
                .withDetail("database", "PostgreSQL")
                .withDetail("status", "Unavailable")
                .withDetail("error", "Unable to establish connection")
                .build();
    }
}
