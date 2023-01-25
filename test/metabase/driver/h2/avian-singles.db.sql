-- Create a non-admin account 'GUEST' which will be used from here on out
CREATE USER IF NOT EXISTS GUEST PASSWORD 'guest';

-- Set DB_CLOSE_DELAY here because only admins are allowed to do it, so we can't set it via the connection string.
-- Set it to to -1 (no automatic closing)
SET DB_CLOSE_DELAY -1;

DROP TABLE IF EXISTS "USERS";

CREATE TABLE "USERS" (
  "ID" BIGINT AUTO_INCREMENT,
  "NAME" VARCHAR,
  PRIMARY KEY ("ID")
);

;

GRANT ALL ON "USERS" TO GUEST;

DROP TABLE IF EXISTS "MESSAGES";

CREATE TABLE "MESSAGES" (
  "ID" BIGINT AUTO_INCREMENT,
  "SENDER_ID" INTEGER,
  "RECEIVER_ID" INTEGER,
  "TEXT" VARCHAR,
  PRIMARY KEY ("ID")
);

;

GRANT ALL ON "MESSAGES" TO GUEST;

ALTER TABLE
  "MESSAGES"
ADD
  CONSTRAINT "GES_SENDER_ID_USERS_-610421431" FOREIGN KEY ("SENDER_ID") REFERENCES "USERS" ("ID");

ALTER TABLE
  "MESSAGES"
ADD
  CONSTRAINT "_RECEIVER_ID_USERS_-1521680153" FOREIGN KEY ("RECEIVER_ID") REFERENCES "USERS" ("ID");

-- 8 rows
INSERT INTO "USERS" ("NAME")
VALUES
('Rasta Toucan'),
('Lucky Pigeon'),
('Peter Pelican'),
('Bob the Sea Gull'),
('Tom Turkey Duck'),
('Ronald Raven'),
('Brenda Blackbird'),
('Annie Albatross');

-- 100 rows
INSERT INTO "MESSAGES" ("SENDER_ID", "RECEIVER_ID", "TEXT")
VALUES
(8, 7, 'Coo'),
(8, 3, 'Bip bip bip bip'),
(3, 2, 'Coo'),
(4, 7, 'Oh hey ;)'),
(1, 8, 'Squawk'),
(6, 7, 'Squawk'),
(8, 6, 'Bip bip bip bip'),
(3, 7, 'Oh hey ;)'),
(3, 1, 'Cawwww!'),
(3, 8, 'Squawk'),
(2, 8, 'Squawk'),
(1, 7, 'Hoooo Hoooo'),
(7, 8, 'Bip bip bip bip'),
(2, 4, 'Coo'),
(8, 2, 'Good day sir!'),
(6, 4, 'Hoooo Hoooo'),
(8, 4, 'Cawwww!'),
(4, 7, 'Hoooo Hoooo'),
(2, 6, 'Squawk'),
(7, 3, 'Cheep'),
(7, 3, 'Oh hey ;)'),
(6, 7, 'Coo'),
(3, 1, 'Hoooo Hoooo'),
(6, 4, 'Bip bip bip bip'),
(6, 2, 'Oh hey ;)'),
(4, 2, 'Bip bip bip bip'),
(8, 2, 'Good day sir!'),
(6, 8, 'Oh hey ;)'),
(3, 4, 'Coo'),
(7, 2, 'Cheep'),
(7, 4, 'Chirp chirp'),
(3, 8, 'Coo'),
(1, 8, 'Squawk'),
(1, 6, 'Cheep'),
(4, 1, 'Bip bip bip bip'),
(2, 5, 'Coo'),
(5, 3, 'Good day sir!'),
(6, 3, 'Cawwww!'),
(1, 6, 'Cheep'),
(5, 4, 'Hoooo Hoooo'),
(4, 1, 'Coo'),
(1, 6, 'Hoooo Hoooo'),
(7, 2, 'Hoooo Hoooo'),
(5, 6, 'Cawwww!'),
(5, 2, 'Squawk'),
(7, 2, 'Good day sir!'),
(1, 5, 'Good day sir!'),
(7, 1, 'Hoooo Hoooo'),
(1, 3, 'Good day sir!'),
(2, 7, 'Cheep'),
(1, 3, 'Hoooo Hoooo'),
(4, 3, 'Hoooo Hoooo'),
(4, 5, 'Cawwww!'),
(3, 7, 'Squawk'),
(4, 5, 'Squawk'),
(8, 5, 'Good day sir!'),
(5, 4, 'Oh hey ;)'),
(1, 4, 'Chirp chirp'),
(5, 6, 'Oh hey ;)'),
(2, 1, 'Oh hey ;)'),
(5, 8, 'Coo'),
(3, 1, 'Good day sir!'),
(7, 6, 'Chirp chirp'),
(6, 7, 'Chirp chirp'),
(5, 3, 'Squawk'),
(8, 2, 'Chirp chirp'),
(6, 3, 'Bip bip bip bip'),
(5, 6, 'Coo'),
(2, 3, 'Good day sir!'),
(3, 1, 'Good day sir!'),
(8, 4, 'Hoooo Hoooo'),
(1, 3, 'Squawk'),
(1, 3, 'Squawk'),
(1, 5, 'Hoooo Hoooo'),
(5, 7, 'Cheep'),
(6, 7, 'Cheep'),
(1, 5, 'Coo'),
(7, 2, 'Coo'),
(4, 3, 'Hoooo Hoooo'),
(4, 7, 'Chirp chirp'),
(5, 7, 'Cheep'),
(7, 5, 'Oh hey ;)'),
(3, 6, 'Chirp chirp'),
(1, 6, 'Bip bip bip bip'),
(2, 1, 'Cheep'),
(6, 7, 'Coo'),
(3, 1, 'Coo'),
(3, 7, 'Good day sir!'),
(2, 5, 'Good day sir!'),
(6, 7, 'Good day sir!'),
(8, 6, 'Squawk'),
(1, 8, 'Good day sir!'),
(1, 4, 'Oh hey ;)'),
(6, 1, 'Good day sir!'),
(7, 3, 'Bip bip bip bip'),
(2, 4, 'Good day sir!'),
(7, 1, 'Cawwww!'),
(2, 5, 'Good day sir!'),
(5, 7, 'Chirp chirp'),
(3, 2, 'Chirp chirp');
