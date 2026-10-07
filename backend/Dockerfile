FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q dependency:go-offline
COPY src src
RUN ./mvnw -q package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --uid 1001 futslot && mkdir -p /data/media && chown futslot /data/media
COPY --from=build /app/target/*.jar app.jar
USER futslot
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
