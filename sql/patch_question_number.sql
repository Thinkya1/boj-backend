-- Add column before backfill if it does not exist
-- ALTER TABLE question ADD COLUMN questionNumber int NULL;

-- Backfill questionNumber from 1000 based on createTime
SET @rownum := 0;
UPDATE question
SET questionNumber = (@rownum := @rownum + 1) + 999
WHERE questionNumber IS NULL OR questionNumber = 0
ORDER BY createTime, id;

-- Optional: enforce uniqueness after backfill
ALTER TABLE question MODIFY questionNumber int NOT NULL;
CREATE UNIQUE INDEX idx_questionNumber ON question(questionNumber);
