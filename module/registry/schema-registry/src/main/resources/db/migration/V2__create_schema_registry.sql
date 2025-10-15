create table if not exists schema_registry (
                                               id         bigserial primary key,
                                               name       varchar(255) not null,
    format     varchar(50)  not null,
    content    text         not null,
    version    integer      not null,
    created_at timestamptz  not null default now()
    );

create unique index if not exists ux_schema_name_version on schema_registry(name, version);
create index if not exists ix_schema_name on schema_registry(name);
