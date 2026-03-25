# Alexgls Messenger
Современный распределенный мессенджер на микросервисной архитектуре с интеграцией искусственного интеллекта для анализа контента.
## Ключевые возможности
* **AI-Аналитика:** Автоматический анализ содержимого документов и изображений с помощью **GigaChat API.** Получение кратких сводок (summary) и распознавание текста.
* **Умный поиск:** Полнотекстовый поиск по истории сообщений и содержимому файлов с использованием **Elasticsearch**.
* **Real-time взаимодействие**: Мгновенная доставка сообщений и системных уведомлений через **WebSockets (STOMP)** и **Redis Presence.**
* **Безопасность:** Авторизация на базе **JWT**, шифрование контента и безопасное хранение вложений.
* **Облачное хранилище:** Масштабируемая работа с файлами через **S3-совместимые хранилища** (Presigned URLs для минимизации нагрузки на бэкенд).
##  Технологический стек
### Backend (Spring Cloud)
* **Язык:** Java 17+
* **Фреймворк:** Spring Boot 3.x, Spring Cloud Gateway
* **Шина данных:** Apache Kafka (Event-Driven Architecture)
* **Базы данных:** PostgreSQL (основные данные), Redis (Online статус)
* **Поисковый движок:** Elasticsearch (индексация метаданных)
* **Безопасность:** Spring Security, OAuth2 Resource Server (JWT)
### Frontend
* **Библиотека:** React
* **Стилизация: CSS3** (переменные, адаптивная верстка, Dark Mode)
### Инфраструктура
* **Оркестрация:** Docker, Docker Compose
* **Мониторинг:** Prometheus + Grafana (настроенные дашборды для мониторинга JVM и нагрузки)
##  Результаты нагрузочного тестирования
Система прошла успешное тестирование с помощью k6 на платформе с I3 12100 и 32Гб ОЗУ:
* **Стабильность:** 0% ошибок при нагрузке 500 одновременных пользователей.
* **Производительность:** Обработка более 275 запросов в секунду (RPS) на одном узле.
* **Latency:** Среднее время ответа при штатной нагрузке — 75 мс.
##Запуск проекта
### 1. Подготовка
   Перед запуском создайте файл .env в корне проекта и укажите необходимые ключи:\
   ``POSTGRES_USER=name ``\
``POSTGRES_PASSWORD=password  ``\
``ELASTIC_USERNAME=name ``\
``ELASTIC_PASSWORD=password ``\
``SBER_AUTH="Токен для GigaChat" ``\
``AES_KEY=ключ_шифрования``\
``HMAC_KEY=ключ для_хеширования_паролей``\
``CONNECTIONS_REDIS_PASSWORD=пароль_redis``\
``AWS_BUCKET_NAME=s3_bucket_name``\
``AWS_ACCESS_KEY=s3_bicket_key``\
``AWS_SECRET_ACCESS_KEY=secret_key``\
``CORS_ALLOWED_ORIGINS=Разрешенные_адреса ``
### 2. Backend & Инфраструктура
  Соберите исполняемые файлы и запустите контейнеры:\
  `` # Сборка всех микросервисов ``\
``mvn clean package -DskipTests``

``## Запуск инфраструктуры (БД, Kafka, Elastic) и сервисов``\
``docker compose -f docker-compose.prod.yaml up -d ``\

### 3. Frontend
``cd messenger-ui``\
``npm install``\
``npm run dev ``\
Приложение будет доступно по адресу: http://localhost:5173
## Архитектура системы
![Архитектурная схема](https://github.com/Alexgls696/Messenger-alexgls/blob/master/images/Architecture.png)

## Скриншоты: 
На скриншотах продемонстрирован UI в темной теме.
### Окно чатов
![Окно чатов](https://github.com/Alexgls696/Messenger-alexgls/blob/master/images/chats_list.png)
### Открытый чат и отправка файла с анализом содержимого
![Открытый чат и отправка файла с анализом содержимого](https://github.com/Alexgls696/Messenger-alexgls/blob/master/images/send_to_analyse.png)
### Результат анализа во вложениях
![Результат анализа во вложениях](https://github.com/Alexgls696/Messenger-alexgls/blob/master/images/analyse_result.png)
### Поиск по содержимом
![Поиск по содержимому](https://github.com/Alexgls696/Messenger-alexgls/blob/master/images/search.png)
### Профиль пользователя
![Профиль пользователя](https://github.com/Alexgls696/Messenger-alexgls/blob/master/images/profile.png)
### Профиль группы
![Профиль группы](https://github.com/Alexgls696/Messenger-alexgls/blob/master/images/group_profile.png)
### Окно логина
![Окно логина](https://github.com/Alexgls696/Messenger-alexgls/blob/master/images/login.png)
