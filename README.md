# Marketly — Digital Marketing Studio

A small, dependency-free Java web app for viewing a digital marketing dashboard and adding campaigns. The responsive interface supports searching and filtering campaigns, sorting by budget, and switching between light and dark themes.

Campaign data is kept in memory and resets when the app restarts. Website visitor and lead figures are sample dashboard metrics.

## Run with Java

Requires Java 17 or newer. In PowerShell, from the project folder:

```powershell
New-Item -ItemType Directory -Force out
javac --add-modules jdk.httpserver -d out src\main\java\com\digitalmarket\DigitalMarketingApp.java
java --add-modules jdk.httpserver -cp out com.digitalmarket.DigitalMarketingApp
```

Open [http://localhost:8080](http://localhost:8080). To select another port, pass it after the class name.

## Run with Docker Compose

Requires Docker Desktop with Docker Compose. Build and start the app locally:

```powershell
docker compose up --build
```

Open [http://localhost:8080](http://localhost:8080). Stop it with `Ctrl+C`, or run `docker compose down` in another terminal.

## Build and publish the Docker image

Sign in to Docker Hub, then build and push `kiransai006/digimarketing`:

```powershell
docker login
docker build -t kiransai006/digimarketing:latest .
docker push kiransai006/digimarketing:latest
```

The Compose file builds the image locally and tags it as `kiransai006/digimarketing:latest`.
