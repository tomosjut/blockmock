#!/bin/bash

echo "Starting BlockMock..."
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "Error: Docker is not running. Please start Docker first."
    exit 1
fi

# Start PostgreSQL
echo "Starting PostgreSQL..."
docker-compose up -d postgres

# Wait for PostgreSQL to be ready
echo "Waiting for PostgreSQL to be ready..."
until docker-compose exec -T postgres pg_isready -U blockmock > /dev/null 2>&1; do
    sleep 1
done

echo "PostgreSQL is ready!"
echo ""

# Start Quarkus in dev mode
echo "Starting Quarkus application..."
mvn quarkus:dev
