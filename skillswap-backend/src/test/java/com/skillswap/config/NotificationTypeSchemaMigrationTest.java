package com.skillswap.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationTypeSchemaMigrationTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final NotificationTypeSchemaMigration migration =
            new NotificationTypeSchemaMigration(jdbcTemplate);
    private final ApplicationArguments arguments =
            mock(ApplicationArguments.class);

    @Test
    void convertsEnumColumnToStringColumn() {
        when(jdbcTemplate.queryForObject(
                any(String.class),
                eq(String.class),
                eq("notifications"),
                eq("type"))).thenReturn("enum");

        migration.run(arguments);

        verify(jdbcTemplate).execute(
                "ALTER TABLE notifications "
                        + "MODIFY COLUMN type VARCHAR(50) NOT NULL");
    }

    @Test
    void leavesExistingStringColumnUnchanged() {
        when(jdbcTemplate.queryForObject(
                any(String.class),
                eq(String.class),
                eq("notifications"),
                eq("type"))).thenReturn("varchar");

        migration.run(arguments);

        verify(jdbcTemplate, never()).execute(any(String.class));
    }
}
