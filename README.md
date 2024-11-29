# Чат-бот VK для сообщества VK Education Projects


## Быстрые ссылки
Документы:
- [Ссылка на сгенерированный READ.ME](https://education-bot.github.io/chatbot_backend/) 
- [План проекта - open Project](https://bot.openproject.com/projects/demo-project/gantt?query_props=%7B%22c%22%3A%5B%22id%22%2C%22type%22%2C%22subject%22%2C%22status%22%2C%22startDate%22%2C%22dueDate%22%2C%22duration%22%5D%2C%22hi%22%3Atrue%2C%22g%22%3A%22%22%2C%22is%22%3Atrue%2C%22tv%22%3Atrue%2C%22tll%22%3A%22%7B%5C%22left%5C%22%3A%5C%22startDate%5C%22%2C%5C%22right%5C%22%3A%5C%22dueDate%5C%22%2C%5C%22farRight%5C%22%3A%5C%22subject%5C%22%7D%22%2C%22tzl%22%3A%22auto%22%2C%22hla%22%3A%5B%22status%22%2C%22priority%22%2C%22dueDate%22%5D%2C%22t%22%3A%22startDate%3Aasc%22%2C%22f%22%3A%5B%7B%22n%22%3A%22status%22%2C%22o%22%3A%22*%22%2C%22v%22%3A%5B%5D%7D%5D%2C%22ts%22%3A%22PT0S%22%2C%22pp%22%3A20%2C%22pa%22%3A1%7D&name=all_open)
- [Техническое Задание - docs](https://docs.google.com/document/d/19xX0_TzsUeh8anVBXrrmLQitK8rfg4-bTEZgxep1v80/edit?tab=t.0#heading=h.smosd6gnwlpe)
- [Записи со встреч - docs](https://docs.google.com/document/d/1x_vvz8FY1ag239mr-hvxhA8hoBHkD0Q-6FLbSfdC5pE/edit?tab=t.0#heading=h.7lmazdrmff9o)
- [Исходный код - github](https://github.com/Education-bot/chatbot_backend)
- [Инфра - Yandex Cloud](https://console.yandex.cloud/folders/b1gbae6rrn6e2e7l80pc)
- [Доска - github](https://github.com/orgs/Education-bot/projects/2)


## Договорённости в команде
### Работа с Git
🧠 Есть предложение работать согласно подходу trunk-based-development для 
работы с нашим git-репозиторием. Вот как заводим новые PR'ы:
1. Создание ветки: Отводите ветку от основной с помощью: git checkout -b bot-{идентификатор issue}. Пример: для задачи с ID=5 используйте bot-5.
2. Коммиты: Вносите изменения и коммитьте их: git commit -m "feat #5: добавил новую логику". #5 означает, что работа ведётся в рамках задачи номер 5. Подробнее можно почитать [тут](https://www.conventionalcommits.org/ru)
3. Пуш: Загружайте изменения: git push origin название_ветки.
4. Пулл-реквест: Создайте пулл-реквест на GitHub и упоминайте в чате кого-то для ревью.
💥 Важно: Не пушим в master/main напрямую, иначе такие коммиты будем реверетить!


## How-To
### Как задеплоить руками на виртуалку?
1. Скачать утилиту yc и docker, авторизоваться в docker-registry. Инструкция в помощь: https://yandex.cloud/ru/docs/container-registry/quickstart/?from=int-console-empty-state#registry-create
```
yc init
yc container registry configure-docker
```

2. Собрать jar (booJar в gradle)
3. Собрать образ, указав платформу linux, в качестве тега указываем дату в формате YYYY-MM-DD (нпример, 2024-11-28):
```
docker build --platform linux/amd64 -t cr.yandex/crpedt3c6ei2sjstcjin/education-bot:{DATE_TAG} .
```
4. Запушить в реджистри:
```
docker push cr.yandex/crpedt3c6ei2sjstcjin/education-bot:{DATE_TAG}
```
5. Заходим на виртуалку:
```
yc compute ssh --id fv4q9dp8btooloqf9hvk
```
6. В домашней директории ищем docker-compose.yaml и запускаем его, предварительно стопнув предыдущий:
```
docker-compose stop
docker-compose-up
```


## Задание от заказчика
**Чат-бот для сообщества**
Цель проекта:
Разработать чат-бота на базе ВКонтакте, который отвечает на вопросы пользователей по VK Education Projects

Описание проекта (проектная задача):
VK Education Projects — витрина проектов для студентов. Проекты могут быть использованы для выполнения домашних заданий, научно-исследовательских, курсовых и дипломных работ. 

Ежедневно команде поступает более 50 сообщений относительно сроков, формата, организационных деталей. Например, студенты спрашивают, как им выбрать проект, какой файл загрузить в качестве решения, где найти информацию о вебинарах.

Задача — разработать чат-бота для ответов на вопросы на базе ВКонтакте. 

Обязательные требования к боту:
Разработан и функционирует на базе ВКонтакте.
Умеет анализировать информацию с официального сайта VK Education Projects и генерировать на её основе ответы пользователям.
Если не знает ответа, отвечает скриптами, которые позволяют пользователям найти информацию самостоятельно.

Дополнительные требования к боту: 
Умеет отвечать на закрытые вопросы (да/нет). Например: «Возможно ли взять несколько проектов?» (Да.)
Способен анализировать открытые источники из интернета в случае, если вопросы пользователей не относятся к сайту VK Education Projects.
Выдаёт предупреждающие сообщения, если в тексте вопросов содержится нецензурная брань или некорректные высказывания.

Материалы для выполнения проекта:
dev.vk.com/ru/api/bots/getting-started — инструкция по созданию чат-ботов

Какие навыки и компетенции нужны для выполнения:
PHP, Java, JavaScript, API, работа с сообществами 

