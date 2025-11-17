#!/bin/bash

echo "Starting BlockMock..."
echo ""

# Detect container runtime (Docker or Podman)
CONTAINER_RUNTIME=""
COMPOSE_CMD=""

if command -v podman &> /dev/null; then
    # Check if Podman is available
    if podman info > /dev/null 2>&1; then
        CONTAINER_RUNTIME="podman"
        # Try podman compose first (newer versions), fall back to podman-compose
        if podman compose version &> /dev/null; then
            COMPOSE_CMD="podman compose"
        elif command -v podman-compose &> /dev/null; then
            COMPOSE_CMD="podman-compose"
        else
            echo "Warning: Podman found but neither 'podman compose' nor 'podman-compose' is available."
            echo "Please install podman-compose: pip3 install podman-compose"
            echo "Falling back to Docker..."
        fi
    fi
fi

if [ -z "$CONTAINER_RUNTIME" ] && command -v docker &> /dev/null; then
    # Check if Docker is available and running
    if docker info > /dev/null 2>&1; then
        CONTAINER_RUNTIME="docker"
        COMPOSE_CMD="docker-compose"
    fi
fi

if [ -z "$CONTAINER_RUNTIME" ]; then
    echo "Error: Neither Docker nor Podman is available or running."
    echo ""
    echo "Please install and start one of the following:"
    echo "  - Docker: https://docs.docker.com/get-docker/"
    echo "  - Podman: https://podman.io/getting-started/installation"
    exit 1
fi

echo "Using container runtime: $CONTAINER_RUNTIME"
echo "Using compose command: $COMPOSE_CMD"
echo ""

# Start PostgreSQL
echo "Starting PostgreSQL..."
$COMPOSE_CMD up -d postgres

# Wait for PostgreSQL to be ready
echo "Waiting for PostgreSQL to be ready..."
if [ "$CONTAINER_RUNTIME" = "podman" ]; then
    # Podman might need different syntax for exec
    until $COMPOSE_CMD exec -T postgres pg_isready -U blockmock > /dev/null 2>&1 || \
          podman exec blockmock-postgres pg_isready -U blockmock > /dev/null 2>&1; do
        sleep 1
    done
else
    until $COMPOSE_CMD exec -T postgres pg_isready -U blockmock > /dev/null 2>&1; do
        sleep 1
    done
fi

echo "PostgreSQL is ready!"
echo ""

# Start Quarkus in dev mode
echo "Starting Quarkus application..."
mvn quarkus:dev
