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

echo "2. Test POST /api/users (Create Regular User - default response)"
echo "--------------------------------------"
curl -X POST "${BASE_URL}/mock/http/api/users" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{"name": "John Doe", "email": "john@example.com", "role": "developer"}' \
  -w "\nStatus: %{http_code}\n"
echo ""
echo ""

echo "3. Test POST /api/users (Create Admin User - matchBody: admin)"
echo "--------------------------------------"
curl -X POST "${BASE_URL}/mock/http/api/users" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{"name": "Admin User", "email": "admin@example.com", "role": "admin"}' \
  -w "\nStatus: %{http_code}\n"
echo ""
echo ""

echo "4. Test POST /api/users (Create Premium User - matchBody: regex)"
echo "--------------------------------------"
curl -X POST "${BASE_URL}/mock/http/api/users" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{"name": "Premium User", "email": "premium@example.com", "premium": true}' \
  -w "\nStatus: %{http_code}\n"
echo ""
echo ""

echo "5. Test POST /api/users (Invalid Email - matchBody: validation)"
echo "--------------------------------------"
curl -X POST "${BASE_URL}/mock/http/api/users" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{"name": "Bad User", "email": "notanemail"}' \
  -w "\nStatus: %{http_code}\n"
echo ""
echo ""

echo "6. Test GET /api/users/123 (Get Specific User)"
echo "--------------------------------------"
curl -X GET "${BASE_URL}/mock/http/api/users/123" \
  -H "Accept: application/json" \
  -w "\nStatus: %{http_code}\n"
echo ""
echo ""

echo "7. Test PUT /api/users/123 (Update User - default)"
echo "--------------------------------------"
curl -X PUT "${BASE_URL}/mock/http/api/users/123" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{"name": "John Doe Updated", "email": "john.doe@example.com"}' \
  -w "\nStatus: %{http_code}\n"
echo ""
echo ""

echo "8. Test PUT /api/users/123 (Suspend User - matchBody: suspend)"
echo "--------------------------------------"
curl -X PUT "${BASE_URL}/mock/http/api/users/123" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{"status": "suspended", "reason": "Violation of terms"}' \
  -w "\nStatus: %{http_code}\n"
echo ""
echo ""

echo "9. Test PUT /api/users/123 (Activate User - matchBody: activate)"
echo "--------------------------------------"
curl -X PUT "${BASE_URL}/mock/http/api/users/123" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{"status": "active"}' \
  -w "\nStatus: %{http_code}\n"
echo ""
echo ""

echo "10. Test PUT /api/users/123 (Try Role Change - should get 403)"
echo "--------------------------------------"
curl -X PUT "${BASE_URL}/mock/http/api/users/123" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{"role": "admin"}' \
  -w "\nStatus: %{http_code}\n"
echo ""
echo ""

echo "11. Test PATCH /api/users/123 (Update Email - matchBody: email)"
echo "--------------------------------------"
curl -X PATCH "${BASE_URL}/mock/http/api/users/123" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{"email": "newemail@example.com"}' \
  -w "\nStatus: %{http_code}\n"
echo ""
echo ""

echo "12. Test PATCH /api/users/123 (Update Password - matchBody: password)"
echo "--------------------------------------"
curl -X PATCH "${BASE_URL}/mock/http/api/users/123" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{"password": "newSecurePassword123"}' \
  -w "\nStatus: %{http_code}\n"
echo ""
echo ""

echo "13. Test PATCH /api/users/123 (Update Name - default response)"
echo "--------------------------------------"
curl -X PATCH "${BASE_URL}/mock/http/api/users/123" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{"name": "Updated Name"}' \
  -w "\nStatus: %{http_code}\n"
echo ""
echo ""

echo "14. Test Error Response (404 - No Match)"
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
