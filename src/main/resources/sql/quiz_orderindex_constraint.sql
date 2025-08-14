-- Add unique constraint to prevent duplicate orderIndex within the same quiz
-- This script should be executed in the database to ensure data integrity

-- First, fix any existing duplicate orderIndex issues
-- Update duplicate orderIndex by assigning sequential numbers
WITH RankedQuestions AS (
    SELECT 
        id,
        quiz_id,
        order_index,
        ROW_NUMBER() OVER (PARTITION BY quiz_id ORDER BY created_at, id) as new_order_index
    FROM quiz_question
    WHERE (quiz_id, order_index) IN (
        SELECT quiz_id, order_index 
        FROM quiz_question 
        GROUP BY quiz_id, order_index 
        HAVING COUNT(*) > 1
    )
)
UPDATE quiz_question 
SET order_index = (
    SELECT new_order_index 
    FROM RankedQuestions 
    WHERE RankedQuestions.id = quiz_question.id
)
WHERE id IN (SELECT id FROM RankedQuestions);

-- Add unique constraint
ALTER TABLE quiz_question 
ADD CONSTRAINT unique_quiz_order_index 
UNIQUE (quiz_id, order_index);

-- Add index for better performance
CREATE INDEX IF NOT EXISTS idx_quiz_questions_order 
ON quiz_question (quiz_id, order_index);

-- Add comment to document the constraint
COMMENT ON CONSTRAINT unique_quiz_order_index ON quiz_question IS 
'Ensures that each question has a unique order index within a quiz'; 