#!/bin/bash

echo "Stopping BlockMock..."
echo ""

# Detect container runtime (Docker or Podman)
COMPOSE_CMD=""

if command -v podman &> /dev/null && podman info > /dev/null 2>&1; then
    # Try podman compose first (newer versions), fall back to podman-compose
    if podman compose version &> /dev/null; then
        COMPOSE_CMD="podman compose"
    elif command -v podman-compose &> /dev/null; then
        COMPOSE_CMD="podman-compose"
    fi
fi

if [ -z "$COMPOSE_CMD" ] && command -v docker &> /dev/null && docker info > /dev/null 2>&1; then
    COMPOSE_CMD="docker-compose"
fi

if [ -z "$COMPOSE_CMD" ]; then
    echo "Error: Neither Docker nor Podman is available."
    exit 1
fi

echo "Using compose command: $COMPOSE_CMD"
echo ""

# Stop and remove containers
echo "Stopping containers..."
$COMPOSE_CMD down

echo ""
echo "BlockMock has been stopped."
echo ""
echo "To remove all data (including database volumes), run:"
echo "  $COMPOSE_CMD down -v"
