-- Clean up duplicate quiz answers and fix data integrity issues
-- This script should be run after the API issues are fixed

-- First, identify and delete duplicate answers for question e040df7d-146b-41ff-af4e-f6a33a6e4fde
-- Keep only the latest answer for each unique content + orderIndex combination

WITH RankedAnswers AS (
    SELECT 
        id,
        question_id,
        answer_text,
        order_index,
        is_correct,
        created_at,
        ROW_NUMBER() OVER (
            PARTITION BY question_id, answer_text, order_index 
            ORDER BY created_at DESC
        ) as rn
    FROM quiz_answer
    WHERE question_id = 'e040df7d-146b-41ff-af4e-f6a33a6e4fde'
)
DELETE FROM quiz_answer 
WHERE id IN (
    SELECT id FROM RankedAnswers WHERE rn > 1
);

-- Clean up any remaining duplicate answers across all questions
WITH DuplicateAnswers AS (
    SELECT 
        id,
        question_id,
        answer_text,
        order_index,
        ROW_NUMBER() OVER (
            PARTITION BY question_id, answer_text, order_index 
            ORDER BY created_at DESC
        ) as rn
    FROM quiz_answer
)
DELETE FROM quiz_answer 
WHERE id IN (
    SELECT id FROM DuplicateAnswers WHERE rn > 1
);

-- Verify cleanup results
SELECT 
    question_id,
    COUNT(*) as total_answers,
    COUNT(CASE WHEN is_correct = true THEN 1 END) as correct_answers
FROM quiz_answer 
GROUP BY question_id
HAVING COUNT(CASE WHEN is_correct = true THEN 1 END) != 1
ORDER BY question_id;

-- Show remaining answers for the problematic question
SELECT 
    id,
    answer_text,
    order_index,
    is_correct,
    created_at
FROM quiz_answer 
WHERE question_id = 'e040df7d-146b-41ff-af4e-f6a33a6e4fde'
ORDER BY order_index, created_at; 