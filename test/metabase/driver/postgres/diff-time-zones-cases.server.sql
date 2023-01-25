DO $$ BEGIN
  PERFORM pg_terminate_backend(pg_stat_activity.pid)
  FROM pg_stat_activity
  WHERE pid <> pg_backend_pid()
    AND pg_stat_activity.datname = 'diff-time-zones-cases';
END $$;

DROP DATABASE IF EXISTS "diff-time-zones-cases";

CREATE DATABASE "diff-time-zones-cases";