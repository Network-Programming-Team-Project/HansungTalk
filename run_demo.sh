#!/bin/bash

# Compile
echo "Compiling..."
javac -d out -sourcepath src src/**/*.java
if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi

# Kill any existing process on port 12345
echo "Checking for existing server on port 12345..."
lsof -ti:12345 | xargs kill -9 2>/dev/null && echo "Killed existing server process"
sleep 1

# Run Server in background
echo "Starting server..."
java -cp out ServerMain &
SERVER_PID=$!
echo "Server started with PID $SERVER_PID"

# Wait for server to be ready
echo "Waiting for server to start..."
sleep 3

# Run Clients
echo "Starting clients..."
java -cp out ui.ClientApp &
java -cp out ui.ClientApp &
java -cp out ui.ClientApp &

echo ""
echo "==================================="
echo "Server and clients are running!"
echo "Press ENTER to stop all processes..."
echo "==================================="
read

# Cleanup
echo "Stopping server..."
kill $SERVER_PID 2>/dev/null
lsof -ti:12345 | xargs kill -9 2>/dev/null
echo "Done!"
