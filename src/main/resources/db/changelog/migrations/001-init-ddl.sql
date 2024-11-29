CREATE TABLE IF NOT EXISTS question
(
    id            BIGSERIAL NOT NULL PRIMARY KEY,
    question_text TEXT      NOT NULL UNIQUE,
    answer        TEXT
);
