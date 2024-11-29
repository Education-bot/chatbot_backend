CREATE TABLE IF NOT EXISTS question
(
    id            BIGSERIAL NOT NULL PRIMARY KEY,
    question_text TEXT      NOT NULL UNIQUE,
    answer        TEXT
);

CREATE TABLE IF NOT EXISTS project
(
    id                     BIGSERIAL NOT NULL PRIMARY KEY,
    name                   TEXT,
    direction              TEXT,
    type                   TEXT,
    min_members            INT,
    max_members            INT,
    goal                   TEXT,
    description            TEXT,
    materials              TEXT,
    selling_description    TEXT,
    algorithm              TEXT,
    competencies           TEXT,
    recommendations        TEXT,
    complexity             TEXT,
    study_format           TEXT,
    intense                TEXT,
    certificate_conditions TEXT,
    expected_result        TEXT,
    grading_criteria       TEXT,
    benefits               TEXT
);
