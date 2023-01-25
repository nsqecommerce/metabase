SET
  SESSION TIMEZONE TO 'UTC';

DROP TABLE IF EXISTS "times";

CREATE TABLE "times" (
  "id" SERIAL,
  "name" TEXT,
  "as_bytes" BYTEA,
  PRIMARY KEY ("id")
);

-- 3 rows
INSERT INTO "times" ("id", "name", "as_bytes")
VALUES
(
  1,
  'foo',
  2023 -01 -25 02 :03 :25,
  371 WARN util.unprepare :: Don 't know how to unprepare values of class [B
[B@7cc51dd9),
(
  2,
  'bar',
  2023 -01 -25 02 :03 :25,
  371 WARN util.unprepare :: Don 't know how to unprepare values of class [B
[B@188b50a5),
(
  3,
  'baz',
  2023 -01 -25 02 :03 :25,
  372 WARN util.unprepare :: Don 't know how to unprepare values of class [B
[B@36c845d6);

