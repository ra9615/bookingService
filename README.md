# SpringProject — Система бронирований отелей

Многомодульный пример распределённого приложения на Spring Boot/Cloud:
- API Gateway (Spring Cloud Gateway)
- Booking Service (JWT-аутентификация, бронирования, согласованность)
- Hotel Management Service (CRUD отелей и номеров, агрегаты по загруженности)
- Eureka Server (Service Registry, динамическое обнаружение сервисов)

Все сервисы используют встроенную БД H2. Взаимодействие между сервисами выполняется как последовательность локальных транзакций (без глобальных распределённых транзакций).

## Возможности
- Регистрация и вход пользователей (JWT) через Booking Service
- Создание бронирований с двухшаговой согласованностью (PENDING → CONFIRMED/CANCELLED с компенсацией)
- Идемпотентность запросов с `requestId`
- Повторы с экспоненциальной паузой и таймауты при удалённых вызовах
- Подсказки по выбору номера (сортировка по `timesBooked`, затем по `id`)
- Администрирование пользователей (CRUD) и отелей/номеров (CRUD) для админов
- Агрегации: популярность номеров по `timesBooked`
- Сквозная корреляция с заголовком `X-Correlation-Id`

## Архитектура и порты
- `eureka-server`: порт 8761
- `api-gateway`: порт 8080
- `hotel-service`: порт случайный (0), регистрируется в Eureka под именем `hotel-service`
- `booking-service`: порт случайный (0), регистрируется в Eureka под именем `booking-service`

Gateway маршрутизирует запросы к сервисам по их serviceId через Eureka и проксирует заголовок `Authorization` (JWT).

## Требования
- Java 17+
- Maven 3.9+

## Сборка и запуск
1) Запустить Eureka:
2) Запустить API Gateway:
3) Запустить Hotel Service и Booking Service (в отдельных терминалах):

## Конфигурация JWT
Для демонстрации используется симметричный ключ HMAC, секрет задаётся свойством `security.jwt.secret` (по умолчанию `dev-secret-please-change`) в:
- `api-gateway/src/main/resources/application.yml`
- `hotel-service/src/main/resources/application.yml`
- `booking-service/src/main/resources/application.yml`


## Основные эндпойнты
Через Gateway (8080):
- Аутентификация (Booking):
  - POST `/auth/register` — регистрация (для админа добавить `"admin": true`)
  - POST `/auth/login` — получение JWT
- Бронирования (Booking):
  - GET `/bookings` — мои бронирования
  - POST `/bookings` — создать бронирование (PENDING → CONFIRMED/компенсация). Поле `createdAt` выставляется автоматически.
  - GET `/bookings/suggestions` — подсказки по комнатам (сортировка по загрузке)
  - GET `/bookings/all` — все бронирования (только админ)
- Пользователи (Booking, admin):
  - GET `/admin/users`, GET `/admin/users/{id}`, PUT `/admin/users/{id}`, DELETE `/admin/users/{id}`
- Отели и номера (Hotel):
  - GET `/hotels`, GET `/hotels/{id}`, POST `/hotels`, PUT `/hotels/{id}`, DELETE `/hotels/{id}` (админ)
  - GET `/rooms/{id}`, POST `/rooms`, PUT `/rooms/{id}`, DELETE `/rooms/{id}` (админ)
  - POST `/rooms/{id}/hold` — удержание слота (идемпотентно по `requestId`)
  - POST `/rooms/{id}/confirm` — подтверждение удержания
  - POST `/rooms/{id}/release` — освобождение удержания (компенсация)
- Статистика (Hotel):
  - GET `/stats/rooms/popular` — номера по популярности (`timesBooked`)

Примечание: схемы DTO упрощены ради демонстрации.

## Согласованность и надёжность
- Локальные транзакции внутри каждого сервиса (`@Transactional`).
- Двухшаговый процесс в Booking: PENDING → (hold в Hotel) → CONFIRM → CONFIRMED; при сбое — RELEASE и `CANCELLED`.
- Идемпотентность по `requestId` (повторные запросы не создают дубликаты и не меняют состояние повторно).
- Повторы с backoff и таймауты в удалённых вызовах к Hotel через `WebClient`.
- Сквозной `X-Correlation-Id` логируется в Booking и прокидывается в Hotel.

## Консоль H2
- Включена для Hotel Service: `/h2-console` (через прямой порт сервиса)
- Схема JDBC: см. `application.yml` соответствующего сервиса

## Swagger / OpenAPI
- Booking Service UI: `http://localhost:<booking-port>/swagger-ui.html`
- Hotel Service UI: `http://localhost:<hotel-port>/swagger-ui.html`
- Gateway (агрегация UI): `http://localhost:8080/swagger-ui.html` (переключатель спецификаций)

## Тестирование
Пример минимального теста в `hotel-service`: `HotelAvailabilityTests` (идемпотентность hold/confirm/release).

### Интеграционные тесты
- Booking Service (WebTestClient + WireMock):
  - `BookingHttpIT#createBooking_Http_Success` — успешное бронирование (CONFIRMED)
  - Покрыты unit/сервис-тестами: ошибка удалённого сервиса (компенсация), таймаут, идемпотентность, подсказки
- Hotel Service (MockMvc):
  - `HotelHttpIT#adminCanCreateHotel` — админ может создавать отели
  - `HotelAvailabilityTests` и `HotelMoreTests` — занятость по датам, available-флаг, статистика

## Ограничения и дальнейшее развитие
- В учебных целях упрощены модели и проверки. Для высокой конкуренции потребуется усилить блокировки/версионирование на уровне БД.
- Без внешнего Identity Provider; при желании интегрируется Keycloak/OAuth2.
- Отказоустойчивость можно усилить Circuit Breaker (Resilience4j), централизованным логированием и трассировкой.
