CREATE TABLE IF NOT EXISTS question
(
    id            BIGSERIAL NOT NULL PRIMARY KEY,
    question_text TEXT      NOT NULL UNIQUE,
    answer        TEXT
);

CREATE TABLE IF NOT EXISTS project (
    id BIGSERIAL NOT NULL PRIMARY KEY,
    name VARCHAR(255),
    direction VARCHAR(255),
    type VARCHAR(255),
    min_members INT,
    max_members INT,
    goal VARCHAR(1000),
    description VARCHAR(1000),
    materials VARCHAR(1000),
    selling_description VARCHAR(1000),
    algorithm VARCHAR(1000),
    competencies VARCHAR(1000),
    recommendations VARCHAR(1000),
    complexity VARCHAR(1000),
    study_format VARCHAR(1000),
    intense VARCHAR(1000),
    certificate_conditions VARCHAR(1000),
    expected_result VARCHAR(1000),
    grading_criteria VARCHAR(1000),
    benefits VARCHAR(1000)
);
