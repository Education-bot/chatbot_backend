package com.vk.education_bot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "unknown_question")
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class UnknownQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private long userId;

    @Column(name = "question_text", nullable = false, unique = true)
    private String questionText;

    public UnknownQuestion(long userId, String questionText) {
        this.userId = userId;
        this.questionText = questionText;
    }
}