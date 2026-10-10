FROM eclipse-temurin:25-jdk AS build
WORKDIR /src
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY src ./src
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon shadowJar -x test

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --create-home --uid 10001 bot
COPY --from=build /src/build/libs/*-all.jar /app/bot.jar
USER bot
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-XX:+UseZGC", "-XX:MaxRAMPercentage=75", "-jar", "/app/bot.jar"]
