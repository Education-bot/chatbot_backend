package com.vk.education_bot.service;

import com.vk.education_bot.entity.Project;
import com.vk.education_bot.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    public Project parseProjectFromMessage(String message) {
        Project project = new Project();

        // Делим текст на части там, где начинается "# " (через lookahead)
        // Если текст не начинается с "# ", в начале может быть пустой кусок, который пропустим.
        String[] sections = message.split("(?=# )");

        for (String section : sections) {
            section = section.trim();
            if (section.isEmpty()) {
                continue;
            }
            // Убираем ведущие "# "
            if (section.startsWith("# ")) {
                section = section.substring(2).trim();
            }

            // Ищем первую перенос строки, чтобы отделить заголовок от содержимого
            int lineBreakPos = section.indexOf('\n');

            // Если в блоке нет переноса строки, значит нет содержимого
            // (или заголовок без текста). Проверим, что lineBreakPos >= 0
            String fieldName;
            String fieldValue;
            if (lineBreakPos < 0) {
                // Весь блок - это только заголовок, без текста
                fieldName = section.toLowerCase();
                fieldValue = "";
            } else {
                fieldName = section.substring(0, lineBreakPos).toLowerCase().trim();
                // Всё остальное (включая переводы строк и абзацы) - поле
                fieldValue = section.substring(lineBreakPos).trim();
            }

            // Теперь сопоставляем fieldName с полями Project
            switch (fieldName) {
                case "название" -> project.setName(fieldValue);
                case "направление" -> project.setDirection(fieldValue);
                case "тип (для чего подходит)" -> project.setType(fieldValue);
                case "минимальное число участников" -> {
                    project.setMinMembers(safeParseInt(fieldValue));
                }
                case "максимальное число участников" -> {
                    project.setMaxMembers(safeParseInt(fieldValue));
                }
                case "цель" -> project.setGoal(fieldValue);
                case "описание" -> project.setDescription(fieldValue);
                case "примеры и материалы" -> project.setMaterials(fieldValue);
                case "продающее описание" -> project.setSellingDescription(fieldValue);
                case "алгоритм" -> project.setAlgorithm(fieldValue);
                case "компетенции" -> project.setCompetencies(fieldValue);
                case "рекомендации" -> project.setRecommendations(fieldValue);
                case "сложность" -> project.setComplexity(fieldValue);
                case "формат обучения" -> project.setStudyFormat(fieldValue);
                case "трудоемкость (в часах)" -> project.setIntense(fieldValue);
                case "условия получения сертификата" -> project.setCertificateConditions(fieldValue);
                case "ожидаемый результат" -> project.setExpectedResult(fieldValue);
                case "критерии оценивания" -> project.setGradingCriteria(fieldValue);
                case "награды" -> project.setBenefits(fieldValue);
                default -> {
                }
            }
        }

        return project;
    }

    private Integer safeParseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Project addProject(Project project) {
        return projectRepository.save(project);
    }

    public Project parseAndAddProject(String adminMessage) {
        var project = parseProjectFromMessage(adminMessage);
        return addProject(project);
    }


    public Project parseAndUpdateProject(Long projectId, String adminMessage) {
        var existingProject = projectRepository.findById(projectId)
            .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));

        var parsedData = parseProjectFromMessage(adminMessage);

        existingProject.setName(parsedData.getName());
        existingProject.setDirection(parsedData.getDirection());
        existingProject.setType(parsedData.getType());
        existingProject.setMinMembers(parsedData.getMinMembers());
        existingProject.setMaxMembers(parsedData.getMaxMembers());
        existingProject.setGoal(parsedData.getGoal());
        existingProject.setDescription(parsedData.getDescription());
        existingProject.setMaterials(parsedData.getMaterials());
        existingProject.setSellingDescription(parsedData.getSellingDescription());
        existingProject.setAlgorithm(parsedData.getAlgorithm());
        existingProject.setCompetencies(parsedData.getCompetencies());
        existingProject.setRecommendations(parsedData.getRecommendations());
        existingProject.setComplexity(parsedData.getComplexity());
        existingProject.setStudyFormat(parsedData.getStudyFormat());
        existingProject.setIntense(parsedData.getIntense());
        existingProject.setCertificateConditions(parsedData.getCertificateConditions());
        existingProject.setExpectedResult(parsedData.getExpectedResult());
        existingProject.setGradingCriteria(parsedData.getGradingCriteria());
        existingProject.setBenefits(parsedData.getBenefits());

        return projectRepository.save(existingProject);
    }

    public String buildProjectDescription(Project project) {
        var sb = new StringBuilder();

        sb.append("# Название\n")
            .append(nullSafe(project.getName())).append("\n");

        sb.append("# Направление\n")
            .append(nullSafe(project.getDirection())).append("\n");

        sb.append("# Тип (для чего подходит)\n")
            .append(nullSafe(project.getType())).append("\n");

        sb.append("# Минимальное число участников\n")
            .append(project.getMinMembers() != null ? project.getMinMembers() : "").append("\n");

        sb.append("# Максимальное число участников\n")
            .append(project.getMaxMembers() != null ? project.getMaxMembers() : "").append("\n");

        sb.append("# Цель\n")
            .append(nullSafe(project.getGoal())).append("\n");

        sb.append("# Описание\n")
            .append(nullSafe(project.getDescription())).append("\n");

        sb.append("# Примеры и материалы\n")
            .append(nullSafe(project.getMaterials())).append("\n");

        sb.append("# Продающее описание\n")
            .append(nullSafe(project.getSellingDescription())).append("\n");

        sb.append("# Алгоритм\n")
            .append(nullSafe(project.getAlgorithm())).append("\n");

        sb.append("# Компетенции\n")
            .append(nullSafe(project.getCompetencies())).append("\n");

        sb.append("# Рекомендации\n")
            .append(nullSafe(project.getRecommendations())).append("\n");

        sb.append("# Сложность\n")
            .append(nullSafe(project.getComplexity())).append("\n");

        sb.append("# Формат обучения\n")
            .append(nullSafe(project.getStudyFormat())).append("\n");

        sb.append("# Трудоемкость (в часах)\n")
            .append(nullSafe(project.getIntense())).append("\n");

        sb.append("# Условия получения сертификата\n")
            .append(nullSafe(project.getCertificateConditions())).append("\n");

        sb.append("# Ожидаемый результат\n")
            .append(nullSafe(project.getExpectedResult())).append("\n");

        sb.append("# Критерии оценивания\n")
            .append(nullSafe(project.getGradingCriteria())).append("\n");

        sb.append("# Награды\n")
            .append(nullSafe(project.getBenefits())).append("\n");

        return sb.toString();
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    public String listAllProjectsAsString() {
        StringBuilder sb = new StringBuilder();
        var projects = projectRepository.findAll();
        projects.sort((o1, o2) -> (int) (o1.getId() - o2.getId()));
        for (Project project : projects) {
            sb.append(project.getId())
                .append(". ")
                .append(project.getName())
                .append("\n");
        }
        return sb.toString();
    }

    public List<Project> getProjects() {
        return projectRepository.findAll();
    }
}