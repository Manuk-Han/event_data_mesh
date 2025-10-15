-- Product 예시 스키마 (MySQL 8.x)
create table if not exists product (
                                       id          bigint auto_increment primary key,
                                       name        varchar(255) not null,
    price       decimal(19,2) not null default 0,
    description text,
    updated_at  datetime(3) not null default current_timestamp(3) on update current_timestamp(3),
    created_at  datetime(3) not null default current_timestamp(3)
    ) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create index if not exists ix_product_updated_at on product(updated_at);
