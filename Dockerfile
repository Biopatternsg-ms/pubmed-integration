FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY target/quarkus-app .

EXPOSE 8080

CMD ["java", "-Dquarkus.http.host=0.0.0.0", "-jar", "quarkus-run.jar"]