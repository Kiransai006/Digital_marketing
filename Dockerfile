FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app
COPY src/main/java/com/digitalmarket/DigitalMarketingApp.java src/main/java/com/digitalmarket/DigitalMarketingApp.java
RUN javac --add-modules jdk.httpserver -d /app/out src/main/java/com/digitalmarket/DigitalMarketingApp.java

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/out /app/out
EXPOSE 8080
USER 10001
ENTRYPOINT ["java", "--add-modules", "jdk.httpserver", "-cp", "/app/out", "com.digitalmarket.DigitalMarketingApp"]
