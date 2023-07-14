SET 'auto.offset.reset' = 'earliest';
CREATE TYPE SEASON_LENGTH AS STRUCT<SEASON_ID INT, EPISODE_COUNT INT>;

CREATE TABLE TITLES
(
    ID          STRING PRIMARY KEY,
    TITLE       STRING,
    ON_SCHEDULE BOOLEAN
) WITH (
      KAFKA_TOPIC='titles',
      VALUE_FORMAT='AVRO'
      );

CREATE STREAM PRODUCTION_CHANGES (
    ROWKEY STRING KEY,
    UUID STRING,
    TITLE_ID STRING,
    CHANGE_TYPE STRING,
    BEFORE SEASON_LENGTH,
    AFTER SEASON_LENGTH,
    CREATED_AT STRING
) WITH (
    KAFKA_TOPIC='production_changes',
    VALUE_FORMAT='AVRO',
    TIMESTAMP='CREATED_AT',
    TIMESTAMP_FORMAT='yyyy-MM-dd HH:mm:ss'
);

CREATE STREAM SEASON_LENGTH_CHANGES
WITH (
  KAFKA_TOPIC='season_length_changes',
  VALUE_FORMAT='AVRO',
  PARTITIONS=5,
  REPLICAS=1
) AS
SELECT ROWKEY,
       TITLE_ID,
       IFNULL(AFTER->SEASON_ID, BEFORE->SEASON_ID) AS SEASON_ID,
       BEFORE->EPISODE_COUNT                       AS OLD_EPISODE_COUNT,
       AFTER->EPISODE_COUNT                        AS NEW_EPISODE_COUNT,
       CREATED_AT
FROM PRODUCTION_CHANGES
WHERE CHANGE_TYPE='season_length' EMIT CHANGES;

CREATE STREAM season_length_changes_enriched
WITH (
    KAFKA_TOPIC='season_length_changes_enriched',
    KEY_FORMAT='JSON',
    VALUE_FORMAT='AVRO',
    PARTITIONS=5,
    TIMESTAMP='created_at',
    TIMESTAMP_FORMAT='yyyy-MM-dd HH:mm:ss'
) AS
SELECT
    s.title_id,
    t.title,
    s.season_id,
    s.old_episode_count,
    s.new_episode_count,
    s.created_at
FROM season_length_changes s
         INNER JOIN titles t
                    ON s.title_id=t.id
EMIT CHANGES ;