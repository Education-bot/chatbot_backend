package com.vk.education_bot.service;

import com.vk.education_bot.entity.Question;
import com.vk.education_bot.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;

    @Autowired
    public QuestionService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public boolean checkQuestionExists(String text) {
        return questionRepository.findByText(text).isPresent();
    }

    public String getAnswer(String text) {
        return questionRepository.findByText(text)
                .map(Question::getAnswer)
                .orElse(null);
    }

    public List<String> getAllQuestions() {
        return questionRepository.findAll().stream()
                .map(Question::getText)
                .toList();
    }

    public void saveQuestion(String text, String answer) {
        Question question = new Question();
        question.setText(text);
        question.setAnswer(answer);
        questionRepository.save(question);
    }

}

