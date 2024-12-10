CREATE TABLE IF NOT EXISTS unknown_question
(
    id            BIGSERIAL NOT NULL PRIMARY KEY,
    user_id       BIGINT    NOT NULL,
    question_text TEXT      NOT NULL UNIQUE
);

