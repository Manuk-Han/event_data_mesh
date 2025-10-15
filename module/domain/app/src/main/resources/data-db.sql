CREATE TABLE IF NOT EXISTS mesh_entities
(
    id
    VARCHAR
(
    64
) PRIMARY KEY,
    label VARCHAR
(
    255
),
    attrs_json TEXT
    );

INSERT INTO mesh_entities(id, label, attrs_json)
VALUES ('E-201', 'Alpha', '{"type":"demo"}'),
       ('E-202', 'Beta', '{"type":"demo","team":"core"}');
