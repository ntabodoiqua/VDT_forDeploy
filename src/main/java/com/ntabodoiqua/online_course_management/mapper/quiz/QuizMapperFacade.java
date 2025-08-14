package com.ntabodoiqua.online_course_management.mapper.quiz;

import com.ntabodoiqua.online_course_management.dto.response.quiz.*;
import com.ntabodoiqua.online_course_management.entity.*;
import com.ntabodoiqua.online_course_management.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class QuizMapperFacade {

    @Autowired
    private QuizMapper quizMapper;
    
    @Autowired
    private QuizQuestionMapper quizQuestionMapper;
    
    @Autowired
    private QuizAnswerMapper quizAnswerMapper;
    
    @Autowired
    private QuizAttemptMapper quizAttemptMapper;
    
    @Autowired
    private QuizAttemptAnswerMapper quizAttemptAnswerMapper;
    
    @Autowired
    private UserMapper userMapper;

    public QuizResponse toQuizResponseWithDetails(Quiz quiz) {
        QuizResponse response = quizMapper.toQuizResponse(quiz);
        if (quiz.getQuestions() != null) {
            response.setQuestions(quiz.getQuestions().stream()
                .map(this::toQuizQuestionResponseWithAnswers)
                .collect(Collectors.toList()));
        }
        return response;
    }

    public QuizStudentResponse toQuizStudentResponseWithDetails(Quiz quiz, int userAttempts, int remainingAttempts, boolean canAttempt, boolean hasAccess) {
        QuizStudentResponse response = quizMapper.toQuizStudentResponse(quiz);
        response.setUserAttempts(userAttempts);
        response.setRemainingAttempts(remainingAttempts);
        response.setCanAttempt(canAttempt);
        response.setHasAccess(hasAccess);
        
        if (quiz.getQuestions() != null && hasAccess) {
            response.setQuestions(quiz.getQuestions().stream()
                .map(this::toQuizQuestionStudentResponseWithAnswers)
                .collect(Collectors.toList()));
        }
        return response;
    }

    public QuizAttemptResponse toQuizAttemptResponseWithDetails(QuizAttempt attempt) {
        QuizAttemptResponse response = new QuizAttemptResponse();
        
        // Map basic fields
        response.setId(attempt.getId());
        response.setAttemptNumber(attempt.getAttemptNumber());
        response.setStartedAt(attempt.getStartedAt());
        response.setSubmittedAt(attempt.getSubmittedAt());
        response.setCompletedAt(attempt.getCompletedAt());
        response.setStatus(attempt.getStatus());
        response.setScore(attempt.getScore());
        response.setPercentage(attempt.getPercentage());
        response.setIsPassed(attempt.getIsPassed());
        response.setTotalQuestions(attempt.getTotalQuestions());
        response.setCorrectAnswers(attempt.getCorrectAnswers());
        response.setIncorrectAnswers(attempt.getIncorrectAnswers());
        response.setUnansweredQuestions(attempt.getUnansweredQuestions());
        
        // Map student
        if (attempt.getStudent() != null) {
            response.setStudent(userMapper.toUserResponse(attempt.getStudent()));
        }
        
        // Map quiz (without nested objects to avoid circular references)
        if (attempt.getQuiz() != null) {
            response.setQuiz(quizMapper.toQuizResponse(attempt.getQuiz()));
        }
        
        // Map attempt answers
        if (attempt.getAttemptAnswers() != null) {
            response.setAttemptAnswers(attempt.getAttemptAnswers().stream()
                .map(this::toQuizAttemptAnswerResponseWithDetails)
                .collect(Collectors.toList()));
        }
        
        return response;
    }

    private QuizQuestionResponse toQuizQuestionResponseWithAnswers(QuizQuestion question) {
        QuizQuestionResponse response = quizQuestionMapper.toQuizQuestionResponse(question);
        if (question.getAnswers() != null) {
            response.setAnswers(question.getAnswers().stream()
                .map(quizAnswerMapper::toQuizAnswerResponse)
                .collect(Collectors.toList()));
        }
        return response;
    }

    private QuizQuestionStudentResponse toQuizQuestionStudentResponseWithAnswers(QuizQuestion question) {
        QuizQuestionStudentResponse response = quizQuestionMapper.toQuizQuestionStudentResponse(question);
        if (question.getAnswers() != null) {
            response.setAnswers(question.getAnswers().stream()
                .map(quizAnswerMapper::toQuizAnswerStudentResponse)
                .collect(Collectors.toList()));
        }
        return response;
    }

    public QuizAttemptAnswerResponse toQuizAttemptAnswerResponseWithDetails(QuizAttemptAnswer attemptAnswer) {
        QuizAttemptAnswerResponse response = quizAttemptAnswerMapper.toQuizAttemptAnswerResponse(attemptAnswer);
        
        // Map question details
        if (attemptAnswer.getQuestion() != null) {
            response.setQuestion(toQuizQuestionResponseWithAnswers(attemptAnswer.getQuestion()));
        }
        
        // Map selected answer
        if (attemptAnswer.getSelectedAnswer() != null) {
            response.setSelectedAnswer(quizAnswerMapper.toQuizAnswerResponse(attemptAnswer.getSelectedAnswer()));
        }
        
        return response;
    }
} 