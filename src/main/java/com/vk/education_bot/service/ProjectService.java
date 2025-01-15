package com.vk.education_bot.service;

import com.vk.education_bot.entity.Project;
import com.vk.education_bot.entity.Section;
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
    /**
     * Парсим строку, введённую администратором, в формат:
     *    # Название
     *    Многострочное описание названия
     *    (можно несколько абзацев)
     *    # Направление
     *    Текст направления
     *    ...
     *
     * Логика:
     *  1. Разбиваем весь текст по "разделителям", где начинается "# ".
     *  2. Для каждого куска: первая строка - заголовок (после "# "), всё остальное - содержимое.
     *  3. Соответствие заголовков (в lowerCase) полям Project - через switch/case или Map.
     */
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
                    // Неизвестный заголовок - можно залогировать или пропустить
                }
            }
        }

        return project;
    }

    /**
     * Безопасный метод парсинга Integer, чтобы не падать с исключением.
     */
    private Integer safeParseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Сохраняет проект в базу. При условии, что Sequence для id уже настроен так,
     * чтобы новые записи начинались с 53, проблем с заполнением ID не будет.
     */
    public Project addProject(Project project) {
        return projectRepository.save(project);
    }

    /**
     * Упрощённый метод «два в одном»:
     * 1) Парсит проект из сообщения
     * 2) Проставляет соответствующую секцию
     * 3) Сохраняет в БД
     */
    public Project parseAndAddProject(String adminMessage, Section section) {
        Project project = parseProjectFromMessage(adminMessage);
        // Если в сущности Project есть связь с Section (например, поле section_id),
        // то нужно предусмотреть это поле в Project и установить:
        //   project.setSection(section);
        // но в данном коде поля section прямо не видно, поэтому аналогично:
        //   project.setSectionId(section.getId());
        // если у Project есть поле sectionId.

        // Пример, если поле в Project называется sectionId:
        // project.setSectionId(section.getId());

        return addProject(project);
    }

    /**
     * 4. Редактирование (обновление) уже существующего проекта.
     *    - На вход подаётся ID проекта и новое описание (adminMessage).
     *    - С помощью parseProjectFromMessage(...) получаем ОБНОВЛЁННЫЕ данные.
     *    - Записываем их в существующий объект и сохраняем.
     *
     *    Если нужно только обновлять "описание" в широком смысле (то есть все поля),
     *    то просто копируем все поля. Если хотите выбирать какие поля обновлять,
     *    нужно добавить логику проверки.
     */
    public Project parseAndUpdateProject(Long projectId, String adminMessage) {
        Project existingProject = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));

        Project parsedData = parseProjectFromMessage(adminMessage);

        // Копируем все нужные поля. Можно написать вспомогательный метод или библиотеку-маппер.
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

    /**
     * 5. Возвращает полное текстовое описание проекта (String) в том же формате:
     *    # Название
     *    <значение>
     *    # Направление
     *    <значение>
     *    ...
     *    # Награды
     *    <значение>
     *
     *    Это нужно, чтобы, например, вернуть администратору готовое описание.
     */
    public String buildProjectDescription(Project project) {
        StringBuilder sb = new StringBuilder();

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

    /**
     * Утилитный метод на всякий случай, чтобы не печатать null-значения буквально "null".
     */
    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    /**
     * 6. Возвращает строку со всеми проектами в формате:
     *    1. НазваниеПроекта1
     *    2. НазваниеПроекта2
     *    ...
     */
    public String listAllProjectsAsString() {
        StringBuilder sb = new StringBuilder();
        List<Project> projects = projectRepository.findAll();
        projects.sort((o1, o2) -> (int) (o1.getId() - o2.getId()));
        for (Project project : projects) {
            sb.append(project.getId())
                    .append(". ")
                    .append(project.getName())
                    .append("\n");
        }
        return sb.toString();
    }

    public List<Long> getProjectIds() {
        List<Project> projects = projectRepository.findAll();
        List<Long> result = new ArrayList<>();
        for (Project project : projects) {
            result.add(project.getId());
        }
        result.sort((Comparator.comparingLong(o -> o)));
        return result;
    }

    public List<Project> getProjects() {
        return projectRepository.findAll();
    }
}