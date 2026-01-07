# Build stage
FROM gradle:8.10-jdk21-alpine AS builder

WORKDIR /app

# Copy Gradle files
COPY build.gradle settings.gradle* gradle.properties gradlew ./
COPY gradle ./gradle

# Установка прав на выполнение для gradlew
RUN chmod +x gradlew

# Скачивание зависимостей (кэшируется отдельно)
RUN ./gradlew dependencies --no-daemon || true

# Копирование исходного кода
COPY src ./src

# Сборка приложения без тестов
RUN ./gradlew clean build -x test --no-daemon

# Финальный этап для минимального образа
FROM eclipse-temurin:21-jdk-alpine

# Установка рабочей директории
WORKDIR /app

# Создание пользователя для безопасного запуска
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Копирование JAR файла из этапа сборки
COPY --from=builder /app/build/libs/*.jar app.jar

# Настройка Health Check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8084/actuator/health || exit 1

# Открытие порта
EXPOSE 8084

# Запуск приложения
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# Аргументы по умолчанию
CMD ["--spring.profiles.active=docker"]