package com.vk.education_bot.client;

import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.messages.Keyboard;
import com.vk.api.sdk.objects.messages.KeyboardButton;
import com.vk.api.sdk.objects.messages.KeyboardButtonActionText;
import com.vk.api.sdk.objects.messages.KeyboardButtonActionTextType;
import com.vk.api.sdk.objects.messages.KeyboardButtonColor;
import com.vk.education_bot.entity.Section;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
public class VkClient {

    private final VkApiClient vkApiClient;
    private final GroupActor groupActor;

    // example
    public void sendMessage(long userId, String message) {
        // todo create common solve for exception handling
        try {
            vkApiClient
                    .messages()
                    .sendDeprecated(groupActor)
                    .userId(userId)
                    .message(message)
                    .randomId(ThreadLocalRandom.current().nextInt())
                    .execute();
        } catch (ApiException | ClientException e) {
            log.error("Got error when send message to user {}", userId);
            throw new RuntimeException(e);
        }
    }

    public void sendMessageWithKeyboard(long userId, String message, Keyboard keyboard) {
        try {
            vkApiClient.messages()
                    .sendDeprecated(groupActor)
                    .userId(userId)
                    .message(message)
                    .keyboard(keyboard)
                    .randomId(ThreadLocalRandom.current().nextInt())
                    .execute();
        } catch (ApiException | ClientException e) {
            log.error("Error sending message with keyboard to user {}", userId, e);
        }
    }

    // Клавиатура для главного меню
    public Keyboard createMainMenuKeyboard() {
        List<List<KeyboardButton>> allButtons = new ArrayList<>();
        List<KeyboardButton> row = new ArrayList<>();

        row.add(new KeyboardButton()
                .setAction(new KeyboardButtonActionText()
                        .setLabel("Задать общий вопрос")
                        .setType(KeyboardButtonActionTextType.TEXT))
                .setColor(KeyboardButtonColor.POSITIVE));

        row.add(new KeyboardButton()
                .setAction(new KeyboardButtonActionText()
                        .setLabel("Вопрос по проекту")
                        .setType(KeyboardButtonActionTextType.TEXT))
                .setColor(KeyboardButtonColor.POSITIVE));

        allButtons.add(row);
        return new Keyboard().setButtons(allButtons);
    }

    // Клавиатура для возврата назад
    public Keyboard createBackButtonKeyboard() {
        List<List<KeyboardButton>> allButtons = new ArrayList<>();
        allButtons.add(List.of(new KeyboardButton()
                .setAction(new KeyboardButtonActionText()
                        .setLabel("Назад")
                        .setType(KeyboardButtonActionTextType.TEXT))
                .setColor(KeyboardButtonColor.NEGATIVE)));

        return new Keyboard().setButtons(allButtons);
    }

    // Клавиатура для списка разделов
    public Keyboard createSectionsKeyboard(List<Section> sections) {
        List<List<KeyboardButton>> allButtons = new ArrayList<>();
        List<KeyboardButton> currentRow = new ArrayList<>();

        for (int i = 0; i < sections.size(); i++) {
            currentRow.add(new KeyboardButton()
                    .setAction(new KeyboardButtonActionText()
                            .setLabel(sections.get(i).getName())
                            .setType(KeyboardButtonActionTextType.TEXT))
                    .setColor(KeyboardButtonColor.PRIMARY));

            if (currentRow.size() == 2 || i == sections.size() - 1) {
                allButtons.add(currentRow);
                currentRow = new ArrayList<>();
            }
        }

        allButtons.add(List.of(new KeyboardButton()
                .setAction(new KeyboardButtonActionText()
                        .setLabel("Назад")
                        .setType(KeyboardButtonActionTextType.TEXT))
                .setColor(KeyboardButtonColor.NEGATIVE)));

        return new Keyboard().setButtons(allButtons);
    }

    // Клавиатура для Да/Нет
    public Keyboard createYesNoKeyboard() {
        List<List<KeyboardButton>> allButtons = new ArrayList<>();
        List<KeyboardButton> row = new ArrayList<>();

        row.add(new KeyboardButton()
                .setAction(new KeyboardButtonActionText()
                        .setLabel("Да")
                        .setType(KeyboardButtonActionTextType.TEXT))
                .setColor(KeyboardButtonColor.POSITIVE));

        row.add(new KeyboardButton()
                .setAction(new KeyboardButtonActionText()
                        .setLabel("Нет")
                        .setType(KeyboardButtonActionTextType.TEXT))
                .setColor(KeyboardButtonColor.NEGATIVE));

        allButtons.add(row);

        allButtons.add(List.of(new KeyboardButton()
                .setAction(new KeyboardButtonActionText()
                        .setLabel("Назад")
                        .setType(KeyboardButtonActionTextType.TEXT))
                .setColor(KeyboardButtonColor.NEGATIVE)));

        return new Keyboard().setButtons(allButtons);
    }

    // Пустая клавиатура (очистка)
    public Keyboard clearKeyboard() {
        return new Keyboard().setButtons(new ArrayList<>());
    }

}
