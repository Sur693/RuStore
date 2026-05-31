FROM gradle:8.7-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle buildFatJar --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/rustore-server-1.0.0-all.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
