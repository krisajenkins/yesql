-- name: create-person-table!
CREATE TABLE person (
	person_id INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY,
	name VARCHAR(20) UNIQUE NOT NULL,
	age INTEGER NOT NULL
)

-- name: insert-person<!
INSERT INTO person (
	name,
	age
) VALUES (
	:name,
	:age
)

-- name: find-older-than
SELECT *
FROM person
WHERE age > :age

-- name: find-by-age
SELECT *
FROM person
WHERE age IN (:age)

-- name: update-age!
UPDATE person
SET age = :age
WHERE name = :name

-- name: delete-person!
DELETE FROM person
WHERE name = :name

-- name: drop-person-table!
DROP TABLE person
