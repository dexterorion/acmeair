#!/bin/bash

echo "🔄 Starting AcmeAir application..."
cd acmeair-webapp

# Start the application in background
nohup ../gradlew appRun > ../server.log 2>&1 &
SERVER_PID=$!
echo "📝 Server PID: $SERVER_PID"
echo "📋 Server logs: ../server.log"

# Wait for server to start
echo "⏳ Waiting for server to start..."
sleep 10

# Test the endpoints
echo ""
echo "🧪 Testing endpoints..."

echo "1. Testing runtime config:"
curl -s -X GET localhost:8081/rest/info/config/runtime | head -5 || echo "❌ Failed to connect"

echo ""
echo "2. Testing active data service:"
curl -s -X GET localhost:8081/rest/info/config/activeDataService || echo "❌ Failed to connect"

echo ""
echo "3. Testing count customers:"
curl -s -X GET localhost:8081/rest/info/config/countCustomers || echo "❌ Failed to connect"

echo ""
echo "4. Testing count bookings:"
curl -s -X GET localhost:8081/rest/info/config/countBookings || echo "❌ Failed to connect"

echo ""
echo "📊 Server status:"
ps aux | grep -v grep | grep $SERVER_PID || echo "❌ Server not running"

echo ""
echo "📜 Recent server logs:"
tail -20 ../server.log

echo ""
echo "🔴 To stop the server: kill $SERVER_PID"