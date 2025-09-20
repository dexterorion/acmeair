#!/bin/bash

echo "Starting AcmeAir in daemon mode..."

# Kill any existing processes
pkill -f "acmeair-webapp" || true

# Start the application with no input required
cd acmeair-webapp
echo "" | nohup ../gradlew appRun > ../server.log 2>&1 &
SERVER_PID=$!
cd ..

echo "Server started with PID: $SERVER_PID"
echo "Log file: server.log"

# Wait for server to start
echo "Waiting for server to initialize..."
sleep 15

# Test if server is responding
echo "Testing server..."
curl -s http://localhost:8081/rest/info/config/activeDataService || echo "Server not ready yet"

echo "Server should be running at http://localhost:8081"
echo "To stop: pkill -f acmeair-webapp"