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
@Getter
@Setter
@Table(name = "project")
@RequiredArgsConstructor
@AllArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "direction")
    private String direction;

    @Column(name = "type")
    private String type;

    @Column(name = "min_members")
    private Integer minMembers;

    @Column(name = "max_members")
    private Integer maxMembers;

    @Column(name = "goal")
    private String goal;

    @Column(name = "description")
    private String description;

    @Column(name = "materials")
    private String materials;

    @Column(name = "selling_description")
    private String sellingDescription;

    @Column(name = "algorithm")
    private String algorithm;

    @Column(name = "competencies")
    private String competencies;

    @Column(name = "recommendations")
    private String recommendations;

    @Column(name = "complexity")
    private String complexity;

    @Column(name = "study_format")
    private String studyFormat;

    @Column(name = "intense")
    private String intense;

    @Column(name = "certificate_conditions")
    private String certificateConditions;

    @Column(name = "expected_result")
    private String expectedResult;

    @Column(name = "grading_criteria")
    private String gradingCriteria;

    @Column(name = "benefits")
    private String benefits;
}