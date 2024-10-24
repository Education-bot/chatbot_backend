FROM openjdk:22-ea-17-slim
WORKDIR /app
COPY build/libs/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
