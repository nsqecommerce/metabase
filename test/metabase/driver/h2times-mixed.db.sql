
DROP TABLE IF EXISTS "TIMES";

CREATE TABLE "TIMES" (
  "ID" BIGINT AUTO_INCREMENT,
  "INDEX" INTEGER,
  "DT" DATETIME,
  "DT_TZ" TIMESTAMP WITH TIME ZONE,
  "D" DATE,
  "AS_DT" VARCHAR,
  "AS_D" VARCHAR,
  PRIMARY KEY ("ID")
);

;

GRANT ALL ON "TIMES" TO GUEST;

DROP TABLE IF EXISTS "WEEKS";

CREATE TABLE "WEEKS" (
  "ID" BIGINT AUTO_INCREMENT,
  "INDEX" INTEGER,
  "DESCRIPTION" VARCHAR,
  "D" DATE,
  PRIMARY KEY ("ID")
);

;

GRANT ALL ON "WEEKS" TO GUEST;
