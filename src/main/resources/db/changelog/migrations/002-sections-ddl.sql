CREATE TABLE IF NOT EXISTS section
(
    id            BIGSERIAL NOT NULL PRIMARY KEY,
    name          TEXT      NOT NULL UNIQUE
);