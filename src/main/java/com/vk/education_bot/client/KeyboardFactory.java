package com.vk.education_bot.client;

import com.vk.api.sdk.objects.messages.Keyboard;
import com.vk.api.sdk.objects.messages.KeyboardButton;
import com.vk.api.sdk.objects.messages.KeyboardButtonActionText;
import com.vk.api.sdk.objects.messages.KeyboardButtonActionTextType;
import com.vk.api.sdk.objects.messages.KeyboardButtonColor;
import com.vk.education_bot.entity.Section;

import java.util.ArrayList;
import java.util.List;

public class KeyboardFactory {

    // Клавиатура для главного меню
    public static Keyboard createMainMenuKeyboard() {
        List<List<KeyboardButton>> allButtons = new ArrayList<>();
        List<KeyboardButton> row = new ArrayList<>();

        row.add(createButton("Задать общий вопрос", KeyboardButtonColor.POSITIVE));
        row.add(createButton("Вопрос по проекту", KeyboardButtonColor.POSITIVE));
        allButtons.add(row);
        return new Keyboard().setButtons(allButtons);
    }

    // Клавиатура для возврата назад
    public static Keyboard createBackButtonKeyboard() {
        List<List<KeyboardButton>> allButtons = new ArrayList<>();
        allButtons.add(List.of(createButton("Назад", KeyboardButtonColor.NEGATIVE)));

        return new Keyboard().setButtons(allButtons);
    }

    // Клавиатура для списка разделов
    public static Keyboard createSectionsKeyboard(List<Section> sections) {
        List<List<KeyboardButton>> allButtons = createSections(sections);
        allButtons.add(List.of(createButton("Назад", KeyboardButtonColor.NEGATIVE)));

        return new Keyboard().setButtons(allButtons);
    }

    public static Keyboard createAdminSectionsKeyboard(List<Section> sections) {
        List<List<KeyboardButton>> allButtons = createSections(sections);
        allButtons.add(List.of(createButton("В меню", KeyboardButtonColor.NEGATIVE)));

        return new Keyboard().setButtons(allButtons);
    }

    private static List<List<KeyboardButton>> createSections(List<Section> sections) {
        List<List<KeyboardButton>> allButtons = new ArrayList<>();
        List<KeyboardButton> currentRow = new ArrayList<>();

        for (int i = 0; i < sections.size(); i++) {
            currentRow.add(createButton(sections.get(i).getName(), KeyboardButtonColor.PRIMARY));

            if (currentRow.size() == 2 || i == sections.size() - 1) {
                allButtons.add(currentRow);
                currentRow = new ArrayList<>();
            }
        }
        return allButtons;
    }

    // Клавиатура для Да/Нет
    public static Keyboard createYesNoKeyboard() {
        List<List<KeyboardButton>> allButtons = new ArrayList<>();
        List<KeyboardButton> row = new ArrayList<>();

        row.add(createButton("Да", KeyboardButtonColor.POSITIVE));
        row.add(createButton("Нет", KeyboardButtonColor.NEGATIVE));

        allButtons.add(row);

        allButtons.add(List.of(createButton("Назад", KeyboardButtonColor.NEGATIVE)));

        return new Keyboard().setButtons(allButtons);
    }

    // Клавиатура админа
    public static Keyboard createAdminKeyboard() {
        List<List<KeyboardButton>> allButtons = new ArrayList<>();

        List<KeyboardButton> row = new ArrayList<>();
        row.add(createButton("Список вопросов", KeyboardButtonColor.PRIMARY));
        row.add(createButton("Ответить на вопрос", KeyboardButtonColor.PRIMARY));
        row.add(createButton("Удалить вопрос", KeyboardButtonColor.PRIMARY));
        allButtons.add(row);

        row = new ArrayList<>();
        row.add(createButton("Описания проектов", KeyboardButtonColor.PRIMARY));
        row.add(createButton("Добавить проект", KeyboardButtonColor.PRIMARY));
        row.add(createButton("Изменить проект", KeyboardButtonColor.PRIMARY));
        allButtons.add(row);

        row = new ArrayList<>();
        row.add(createButton("Список админов", KeyboardButtonColor.PRIMARY));
        row.add(createButton("Добавить админа", KeyboardButtonColor.POSITIVE));
        row.add(createButton("Удалить админа", KeyboardButtonColor.NEGATIVE));
        allButtons.add(row);

        allButtons.add(List.of(createButton("Назад", KeyboardButtonColor.NEGATIVE)));

        return new Keyboard().setButtons(allButtons);
    }

    public static Keyboard createAdminBackButtonKeyboard() {
        List<List<KeyboardButton>> allButtons = new ArrayList<>();
        allButtons.add(List.of(createButton("В меню", KeyboardButtonColor.NEGATIVE)));

        return new Keyboard().setButtons(allButtons);
    }

    // Пустая клавиатура (очистка)
    public static Keyboard clearKeyboard() {
        return new Keyboard().setButtons(new ArrayList<>());
    }

    private static KeyboardButton createButton(String label, KeyboardButtonColor color) {
        return new KeyboardButton()
            .setAction(new KeyboardButtonActionText()
                .setLabel(label)
                .setType(KeyboardButtonActionTextType.TEXT))
            .setColor(color);
    }
}
