CREATE TABLE task (
id INT AUTO_INCREMENT,
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
started_at TIMESTAMP,
completed_at TIMESTAMP,
status VARCHAR(20) NOT NULL, -- PENDING / RUNNING / COMPLETED / FAILED
attempt INT DEFAULT 0
);