FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app
RUN mkdir -p /app/logs

COPY --from=builder /build/target/fleet-manager.jar /app/fleet-manager.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "fleet-manager.jar"]
