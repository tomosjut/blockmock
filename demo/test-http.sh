#!/bin/bash
# Test script voor HTTP endpoint demo

echo "======================================"
echo "BlockMock HTTP Demo Tests"
echo "======================================"
echo ""

BASE_URL="http://localhost:8888"

echo "1. Test GET /api/users"
echo "--------------------------------------"
curl -X GET "${BASE_URL}/mock/http/api/users" \
  -H "Accept: application/json" \
  -w "\nStatus: %{http_code}\n"
echo ""
echo ""

echo "2. Test POST /api/users (Create User)"
echo "--------------------------------------"
curl -X POST "${BASE_URL}/mock/http/api/users" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{"name": "John Doe", "email": "john@example.com", "role": "developer"}' \
  -w "\nStatus: %{http_code}\n"
echo ""
echo ""

echo "3. Test GET /api/users/123 (Get Specific User)"
echo "--------------------------------------"
curl -X GET "${BASE_URL}/mock/http/api/users/123" \
  -H "Accept: application/json" \
  -w "\nStatus: %{http_code}\n"
echo ""
echo ""

echo "4. Test PUT /api/users/123 (Update User)"
echo "--------------------------------------"
curl -X PUT "${BASE_URL}/mock/http/api/users/123" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{"name": "John Doe Updated", "email": "john.doe@example.com"}' \
  -w "\nStatus: %{http_code}\n"
echo ""
echo ""

echo "5. Test Error Response (404)"
echo "--------------------------------------"
curl -X GET "${BASE_URL}/mock/http/api/nonexistent" \
  -H "Accept: application/json" \
  -w "\nStatus: %{http_code}\n"
echo ""
echo ""

echo "======================================"
echo "HTTP Tests Completed!"
echo "Check Request Logs in UI: ${BASE_URL}"
echo "======================================"
