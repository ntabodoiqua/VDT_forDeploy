package com.ntabodoiqua.online_course_management.mapper.quiz;

import com.ntabodoiqua.online_course_management.dto.request.quiz.QuizCreationRequest;
import com.ntabodoiqua.online_course_management.dto.request.quiz.QuizUpdateRequest;
import com.ntabodoiqua.online_course_management.dto.response.quiz.QuizResponse;
import com.ntabodoiqua.online_course_management.dto.response.quiz.QuizStudentResponse;
import com.ntabodoiqua.online_course_management.dto.response.quiz.QuizSummaryResponse;
import com.ntabodoiqua.online_course_management.entity.Quiz;
import com.ntabodoiqua.online_course_management.mapper.LessonMapper;
import com.ntabodoiqua.online_course_management.mapper.UserMapper;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {UserMapper.class, LessonMapper.class})
public interface QuizMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lesson", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "questions", ignore = true)
    @Mapping(target = "attempts", ignore = true)
    Quiz toQuiz(QuizCreationRequest request);

    @Mapping(target = "questions", ignore = true)
    @Mapping(source = "attempts", target = "totalAttempts", qualifiedByName = "mapAttemptsCount")
    @Mapping(source = "questions", target = "totalQuestions", qualifiedByName = "mapQuestionsCount")
    QuizResponse toQuizResponse(Quiz quiz);

    @Mapping(source = "lesson.id", target = "lessonId")
    @Mapping(source = "lesson.title", target = "lessonTitle")
    @Mapping(source = "questions", target = "totalQuestions", qualifiedByName = "mapQuestionsCount")
    @Mapping(target = "userAttempts", ignore = true)
    @Mapping(target = "remainingAttempts", ignore = true)
    @Mapping(target = "canAttempt", ignore = true)
    @Mapping(target = "hasAccess", ignore = true)
    @Mapping(target = "questions", ignore = true)
    QuizStudentResponse toQuizStudentResponse(Quiz quiz);

    @Mapping(source = "lesson.id", target = "lessonId")
    @Mapping(source = "lesson.title", target = "lessonTitle")
    @Mapping(source = "questions", target = "totalQuestions", qualifiedByName = "mapQuestionsCount")
    @Mapping(source = "attempts", target = "totalAttempts", qualifiedByName = "mapAttemptsCount")
    @Mapping(target = "passedAttempts", ignore = true)
    QuizSummaryResponse toQuizSummaryResponse(Quiz quiz);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "lesson", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "questions", ignore = true)
    @Mapping(target = "attempts", ignore = true)
    void updateQuiz(@MappingTarget Quiz quiz, QuizUpdateRequest request);

    @Named("mapQuestionsCount")
    default int mapQuestionsCount(java.util.Set<?> questions) {
        return questions != null ? questions.size() : 0;
    }

    @Named("mapAttemptsCount")
    default int mapAttemptsCount(java.util.Set<?> attempts) {
        return attempts != null ? attempts.size() : 0;
    }
} 