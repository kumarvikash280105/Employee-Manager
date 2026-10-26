# Stage 1: Build JAR with Maven & Eclipse Temurin JDK 21
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Minimalist JRE 21 Runtime (Optimized for Render 512MB RAM & 0.1 CPU)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/empmanage-cms-1.0.0.jar app.jar
EXPOSE 8090
ENV PORT=8090
ENTRYPOINT ["sh", "-c", "java -XX:+UseSerialGC -Xms128m -Xmx350m -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Dserver.port=${PORT:-8090} -jar app.jar"]
