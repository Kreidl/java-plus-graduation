# Explore With Me
Приложение для публикации информации об интересных событиях и поиска компаний для участия в них.

## О проекте
Explore With Me — это масштабируемая система для управления событиями, пользователями и категориями. Проект реализует микросервисную архитектуру с использованием Spring Cloud, обеспечивая:
🔍 Service Discovery через Eureka
⚙️ Централизованную конфигурацию через Config Server
🚪 API Gateway для маршрутизации запросов
📊 Сбор статистики обращений к эндпоинтам
🗄️ Репозитории данных с PostgreSQL + JPA + QueryDSL

## Архитектура

┌─────────────────────────────────────────────────────────────┐
│                          CLIENT                             │
│                 (Browser / Postman / Mobile)                │
└──────────────────────────┬──────────────────────────────────┘
│ HTTP/HTTPS :8080
▼
┌─────────────────────────────────────────────────────────────┐
│                        🚪 API GATEWAY                       │
│              spring-cloud-starter-gateway                   │
│          • Маршрутизация • CORS • Rate Limiting             │
└───┬────────────┬────────────┬────────────┬────────────┬─────┘
│ lb://      │ lb://      │ lb://      │ lb://      │ lb://
▼            ▼            ▼            ▼            ▼
┌─────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│ EVENT-  │ │  USER-   │ │ REQUEST- │ │ COMMENT- │ │  STATS   │
│ SERVICE │ │ SERVICE  │ │ SERVICE  │ │ SERVICE  │ │  SERVER  │
├─────────┤ ├──────────┤ ├──────────┤ ├──────────┤ ├──────────┤
│• Events │ │• Users   │ │• Requests│ │• Comments│ │• Hits    │
│• Compil.│ │• Admins  │ │• Status  │ │          │ │• Reports │
│• Cat.   │ │          │ │          │ │          │ │• Analytics│
└────┬────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘
│           │            │            │            │
▼           ▼            ▼            ▼            ▼
┌─────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│event-db │ │  user-db │ │request-db│ │comment-db│ │ stats-db │
│ (PG)    │ │  (PG)    │ │  (PG)    │ │  (PG)    │ │  (PG)    │
└─────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘

┌─────────────────────────────────────────────────────────────┐
│                   🔍 DISCOVERY SERVER (Eureka)              │
│                   ⚙️ CONFIG SERVER (Spring Cloud Config)    │
└─────────────────────────────────────────────────────────────┘

### Микросервисы

🎯 Event Service (event-service) — управление событиями
[DataBase Diagram](assets/readme/schemas/event-service-schema.svg)

👫 User Service (user-service) — управление пользователями
[DataBase Diagram](assets/readme/schemas/user-service-schema.svg)

🙋 Request Service (request-service) — управление заявками на участие
[DataBase Diagram](assets/readme/schemas/request-service-schema.svg)

✏️ Comment service (comment-service) — добавление и модерация комментариев
[DataBase Diagram](assets/readme/schemas/comment-service-schema.svg)

📊 Stats Server (stats-server) — сбор и анализ статистики обращений
[DataBase Diagram](assets/readme/schemas/stats-service-schema.svg)

### Инфраструктурные сервисы

🚪 Gateway Server (gateway-server) — единая точка входа в приложение

🔍 Discovery Server (discovery-server) — регистрация и обнаружение сервисов

⚙️ Config Server (config-server) — централизованная конфигурация

## Технологический стек

### Ядро

|  Технология  |  Версия  | Назначение                            |
|:------------:|:--------:|---------------------------------------|
|     Java     |    21    | Язык программирования                 |
| Spring Boot  |  3.3.4   | Фреймворк для микросервисов           |
| Spring Cloud | 2023.0.0 | Инструменты для распределённых систем |
|    Maven     |   3.9+   | Сборка и управление зависимостями     |

### Данные

|    Технология    | Версия  | Назначение                          |
|:----------------:|:-------:|:------------------------------------|
|    PostgreSQL    |   15    | Основная СУБД                       |
| Spring Data JPA  |  3.3.4  | ORM и репозитории                   |
|    Hibernate     |  6.5.3  | Провайдер JPA                       |
|     QueryDSL     |  5.1.0  | Типобезопасные динамические запросы |

### Интеграции

| Технология                  | Назначение                    |
|-----------------------------|-------------------------------|
| Spring Cloud Netflix Eureka | Service Discovery             |
| Spring Cloud Config         | Централизованная конфигурация |
| Spring Cloud Gateway        | API Gateway                   |
| Spring Cloud LoadBalancer   |  Балансировка нагрузки        |

### Инфраструктура

| Инструмент               | Назначение                    |
|--------------------------|-------------------------------|
| Docker & Docker Compose  | Контейнеризация и оркестрация |
| GitHub Actions           | CI/CD пайплайны               |
| Actuator                 | Мониторинг и health-check     |

### Тестирование
| Инструмент |
|------------|
| Postman    |

#### Тесты Postman:

[Тестирование модуля core](postman/ewm-main-service.json)
[тестирование модуля stat](postman/ewm-stat-service.json)

## Внешнее API
Через API Gateway на порту 8080. Все запросы проходят через Gateway.

### Публичные API (доступны всем)
#### Категории:
- GET /categories - получение списка категорий
- GET /categories/{catId} - получение категории по идентификатору

#### События:
- GET /events - получение событий с фильтрацией
- GET /events/{id} - получение подробной информации о событии

####  Подборки событий:
- GET /compilations - получение подборок событий
- GET /compilations/{compId} - получение подборки по идентификатору

### Закрытые API (требуют аутентификации)
#### Пользователи:
- GET /users/{userId} - получение информации о пользователе
- POST /users - регистрация пользователя

#### События:
- POST /users/{userId}/events - создание события
- GET /users/{userId}/events - получение событий пользователя
- PATCH /users/{userId}/events/{eventId} - редактирование события

#### Запросы на участие:
- POST /users/{userId}/requests?eventId={eventId} - создание запроса на участие
- GET /users/{userId}/requests - получение запросов пользователя
- PATCH /users/{userId}/requests/{requestId}/cancel - отмена запроса

#### Комментарии:
- POST /users/{userId}/events/{eventId}/comments - создание комментария
- GET /users/{userId}/events/{eventId}/comments - получение комментариев события

### Административные API
#### Пользователи:
- GET /admin/users - получение всех пользователей
- POST /admin/users - создание пользователя
- DELETE /admin/users/{userId} - удаление пользователя

#### Категории:
- POST /admin/categories - создание категории
- PATCH /admin/categories/{catId} - редактирование категории
- DELETE /admin/categories/{catId} - удаление категории

#### События:
- GET /admin/events - получение всех событий
- PATCH /admin/events/{eventId} - редактирование события (публикация/отклонение)

#### Подборки:
- POST /admin/compilations - создание подборки
- PATCH /admin/compilations/{compId} - редактирование подборки
- DELETE /admin/compilations/{compId} - удаление подборки

### Спецификации:
[Модуль core](/ewm-main-service-spec.json)
[Модуль stat](ewm-stats-service-spec.json)
