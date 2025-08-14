package com.ntabodoiqua.online_course_management.mapper.quiz;

import com.ntabodoiqua.online_course_management.dto.request.quiz.QuizAttemptAnswerRequest;
import com.ntabodoiqua.online_course_management.dto.response.quiz.QuizAttemptAnswerResponse;
import com.ntabodoiqua.online_course_management.entity.QuizAttemptAnswer;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface QuizAttemptAnswerMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "attempt", ignore = true)
    @Mapping(target = "question", ignore = true)
    @Mapping(target = "selectedAnswer", ignore = true)
    @Mapping(target = "isCorrect", ignore = true)
    @Mapping(target = "pointsEarned", ignore = true)
    @Mapping(target = "answeredAt", ignore = true)
    QuizAttemptAnswer toQuizAttemptAnswer(QuizAttemptAnswerRequest request);

    @Mapping(target = "question", ignore = true)
    @Mapping(target = "selectedAnswer", ignore = true)
    QuizAttemptAnswerResponse toQuizAttemptAnswerResponse(QuizAttemptAnswer attemptAnswer);
} 