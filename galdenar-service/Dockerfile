# Steg 1: bygg jar-filen med Maven (JDK). Beroendena hämtas i ett eget lager, så att de
# inte laddas ner igen när bara koden ändras.
FROM eclipse-temurin:25-jdk AS bygg
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q package -DskipTests

# Steg 2: bara det som behövs för att köra (JRE), som en vanlig användare – inte root.
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=bygg /app/target/galdenar-service-0.0.1-SNAPSHOT.jar app.jar
USER 1000
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
