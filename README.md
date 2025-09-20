# Acme Air Sample and Benchmark

This application shows an implementation of a fictitious airline called "Acme Air".  The application was built with the some key business requirements: the ability to scale to billions of web API calls per day, the need to develop and deploy the application in public clouds (as opposed to dedicated pre-allocated infrastructure), and the need to support multiple channels for user interaction (with mobile enablement first and browser/Web 2.0 second).

There are two implementations of the application tier. Each application implementation, supports multiple data tiers.  They are:
- **Node.js**
  - MongoDB
  - Cloudant
- **Java**
  - Jetty with MongoDB (using Morphia driver)

## System Requirements

- **Java**: OpenJDK 21+ (tested with OpenJDK 21.0.8)
- **Gradle**: 8.14.3 (included via Gradle Wrapper)
- **MongoDB**: 3.x or higher
- **Docker**: For running MongoDB in containers (optional)

## Repository Contents

Source:

- **acmeair-common**: The Java entities used throughout the application
- **acmeair-loader**:  A tool to load the Java implementation data store
- **acmeair-services**:  The Java data services interface definitions
- **acmeair-service-morphia**:  A mongodb data service implementation
- **acmeair-webapp**:  The Web 2.0 application and associated Java REST services

## Quick Start Guide

### 1. Start MongoDB

#### Option A: Using Docker (Recommended)
```bash
# Start MongoDB in a Docker container
docker run -d --name acmeair-mongo -p 27017:27017 mongo:latest

# Verify MongoDB is running
docker ps | grep acmeair-mongo
```

#### Option B: Local MongoDB Installation
```bash
# Start MongoDB service (varies by OS)
# macOS (with Homebrew):
brew services start mongodb-community

# Linux (systemd):
sudo systemctl start mongod

# Windows:
net start MongoDB
```

### 2. Build and Run the Application

```bash
# Build the application with MongoDB support
./gradlew -Pservice=morphia clean build

# Start the web application
./gradlew :acmeair-webapp:appRun
```

The application will be available at: http://localhost:8081

### 3. Load Sample Data

In a separate terminal, load the sample data:

```bash
# Load sample data into MongoDB
./gradlew :acmeair-loader:run
```

### 4. Access the Application

Open your browser and navigate to:
- **Main Application**: http://localhost:8081
- **API Endpoints**: http://localhost:8081/rest/api/

## Configuration

The MongoDB connection can be configured in:
`acmeair-services-morphia/src/main/resources/acmeair-mongo.properties`

Default settings:
```properties
mongo.host=localhost
mongo.port=27017
mongo.database=acmeair
```

## Development Commands

```bash
# Clean and build
./gradlew clean build

# Build with specific service (morphia for MongoDB)
./gradlew -Pservice=morphia build

# Run tests
./gradlew test

# Start development server
./gradlew :acmeair-webapp:appRun

# Load sample data
./gradlew :acmeair-loader:run

# Stop all background processes
./gradlew --stop
```

## Docker MongoDB Management

```bash
# Start MongoDB container
docker run -d --name acmeair-mongo -p 27017:27017 mongo:latest

# Stop MongoDB container
docker stop acmeair-mongo

# Remove MongoDB container
docker rm acmeair-mongo

# View MongoDB logs
docker logs acmeair-mongo

# Connect to MongoDB shell
docker exec -it acmeair-mongo mongosh
```

## Troubleshooting

1. **Port 8081 already in use**: Change the port in `build.gradle` under the `gretty` configuration
2. **MongoDB connection issues**: Verify MongoDB is running and accessible on localhost:27017
3. **Java compatibility issues**: Ensure you're using Java 21 or higher
4. **Build failures**: Run `./gradlew clean` before building

## Additional Resources

* Acme Air for Node.js [Instructions](https://github.com/acmeair/acmeair-nodejs/blob/master/README.md)

## Ask Questions

Questions about the Acme Air Open Source Project can be directed to our Google Groups.

* Acme Air Users: [https://groups.google.com/forum/?fromgroups#!forum/acmeair-users](https://groups.google.com/forum/?fromgroups#!forum/acmeair-users)

## Submit a bug report

We use github issues to report and handle bug reports.

## OSS Contributions

We accept contributions via pull requests.

CLA agreements needed for us to accept pull requests soon can be found in the [CLAs directory](CLAs) of the repository.
