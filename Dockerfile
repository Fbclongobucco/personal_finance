FROM eclipse-temurin:25-jdk-jammy AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline

COPY src ./src
RUN ./mvnw -q clean package -DskipTests

FROM eclipse-temurin:25-jre-jammy
WORKDIR /app

RUN useradd --uid 1000 --create-home --shell /usr/sbin/nologin appuser
COPY --from=build /app/target/personal_finance_app-*.jar app.jar
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
