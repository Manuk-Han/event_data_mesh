CREATE TABLE IF NOT EXISTS `member` (
                                        `id` CHAR(36) NOT NULL,
    `email` VARCHAR(255) NOT NULL,
    `name`  VARCHAR(255) NOT NULL,
    `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    `created_at` TIMESTAMP(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at` TIMESTAMP(6) NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_member_email` (`email`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;