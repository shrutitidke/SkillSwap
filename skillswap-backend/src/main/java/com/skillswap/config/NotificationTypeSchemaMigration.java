package com.skillswap.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificationTypeSchemaMigration implements ApplicationRunner {

    private static final Logger logger =
            LoggerFactory.getLogger(NotificationTypeSchemaMigration.class);

    private final JdbcTemplate jdbcTemplate;

    public NotificationTypeSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        String columnType = jdbcTemplate.queryForObject(
                """
                SELECT DATA_TYPE
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """,
                String.class,
                "notifications",
                "type");

        if ("enum".equalsIgnoreCase(columnType)) {
            jdbcTemplate.execute(
                    "ALTER TABLE notifications "
                            + "MODIFY COLUMN type VARCHAR(50) NOT NULL");
            logger.info(
                    "Migrated notifications.type from ENUM to VARCHAR(50)");
        }
    }
}
