#!/usr/bin/env bash
set -e

BASE="${BLOCKMOCK_URL:-http://localhost:8080}/api"

# All info/echo output goes to stderr so stdout is clean for ID capture
info() { echo "$@" >&2; }

find_endpoint() {
  local name="$1"
  curl -sf "$BASE/endpoints" | grep -o "\"id\":[0-9]*,\"name\":\"$name\"" | grep -o '"id":[0-9]*' | cut -d: -f2
}

find_block() {
  local name="$1"
  curl -sf "$BASE/blocks" | grep -o "\"id\":[0-9]*,\"name\":\"$name\"" | grep -o '"id":[0-9]*' | cut -d: -f2
}

find_test_suite() {
  local name="$1"
  curl -sf "$BASE/test-suites" | grep -o "\"id\":[0-9]*,\"name\":\"$name\"" | grep -o '"id":[0-9]*' | cut -d: -f2
}

find_trigger() {
  local name="$1"
  curl -sf "$BASE/triggers" | grep -o "\"id\":[0-9]*,\"name\":\"$name\"" | grep -o '"id":[0-9]*' | cut -d: -f2
}

ensure_endpoint() {
  local name="$1" method="$2" path="$3" description="$4"
  local response_name="$5" response_status="$6" response_body="$7"

  info ""
  local existing
  existing=$(find_endpoint "$name")
  if [ -n "$existing" ]; then
    info "Endpoint \"$name\" already exists (id=$existing), skipping."
    echo "$existing"
    return
  fi

  info "Creating endpoint: $method $path"
  local EP ID
  EP=$(curl -sf -X POST "$BASE/endpoints" \
    -H "Content-Type: application/json" \
    -d "{
      \"name\": \"$name\",
      \"description\": \"$description\",
      \"protocol\": \"HTTP\",
      \"pattern\": \"REQUEST_REPLY\",
      \"enabled\": true,
      \"httpMethod\": \"$method\",
      \"httpPath\": \"$path\",
      \"httpPathRegex\": false
    }")
  ID=$(echo "$EP" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
  info "  -> id=$ID"

  curl -sf -X POST "$BASE/endpoints/$ID/responses" \
    -H "Content-Type: application/json" \
    -d "{
      \"name\": \"$response_name\",
      \"priority\": 0,
      \"responseStatusCode\": $response_status,
      \"responseBody\": $response_body,
      \"responseDelayMs\": 0
    }" > /dev/null
  info "  -> response: $response_name ($response_status)"

  echo "$ID"
}

ensure_block() {
  local name="$1" description="$2" color="$3"
  shift 3
  local endpoint_ids=("$@")

  info ""
  local existing
  existing=$(find_block "$name")
  if [ -n "$existing" ]; then
    info "Block \"$name\" already exists (id=$existing), skipping."
    echo "$existing"
    return
  fi

  info "Creating block: $name"
  local BLOCK BLOCK_ID
  BLOCK=$(curl -sf -X POST "$BASE/blocks" \
    -H "Content-Type: application/json" \
    -d "{\"name\": \"$name\", \"description\": \"$description\", \"color\": \"$color\"}")
  BLOCK_ID=$(echo "$BLOCK" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
  info "  -> id=$BLOCK_ID"

  for EP_ID in "${endpoint_ids[@]}"; do
    curl -sf -X POST "$BASE/blocks/$BLOCK_ID/endpoints/$EP_ID" > /dev/null
    info "  -> added endpoint $EP_ID"
  done

  echo "$BLOCK_ID"
}

ensure_test_suite() {
  local name="$1" description="$2" color="$3" block_id="$4"
  local payment_id="$5" inventory_id="$6" notifications_id="$7"

  info ""
  local existing
  existing=$(find_test_suite "$name")
  if [ -n "$existing" ]; then
    info "Test suite \"$name\" already exists (id=$existing), skipping."
    echo "$existing"
    return
  fi

  info "Creating test suite: $name"
  local SUITE
  SUITE=$(curl -sf -X POST "$BASE/test-suites" \
    -H "Content-Type: application/json" \
    -d "{
      \"name\": \"$name\",
      \"description\": \"$description\",
      \"color\": \"$color\",
      \"blocks\": [{\"id\": $block_id}],
      \"expectations\": [
        {
          \"name\": \"Payment is charged\",
          \"mockEndpoint\": {\"id\": $payment_id},
          \"minCallCount\": 1,
          \"maxCallCount\": 1,
          \"expectationOrder\": 1
        },
        {
          \"name\": \"Inventory is reserved\",
          \"mockEndpoint\": {\"id\": $inventory_id},
          \"minCallCount\": 1,
          \"maxCallCount\": 1,
          \"expectationOrder\": 2
        },
        {
          \"name\": \"Notification is sent\",
          \"mockEndpoint\": {\"id\": $notifications_id},
          \"minCallCount\": 1,
          \"maxCallCount\": 1,
          \"expectationOrder\": 3
        }
      ]
    }")
  local SUITE_ID
  SUITE_ID=$(echo "$SUITE" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
  info "  -> id=$SUITE_ID"
  info "  -> 3 expectations (payment → inventory → notification, sequence enforced)"
  echo "$SUITE_ID"
}

info "Setting up BlockMock demo at $BASE..."

PAYMENT_ID=$(ensure_endpoint \
  "Payment Charge" "POST" "/api/payment/charge" \
  "Charges the customer for an order" \
  "Approved" 200 \
  '"{\"transactionId\": \"tx-abc123\", \"status\": \"approved\", \"amount\": 29.99}"')

INVENTORY_ID=$(ensure_endpoint \
  "Inventory Reserve" "POST" "/api/inventory/reserve" \
  "Reserves stock for an order" \
  "Reserved" 200 \
  '"{\"reservationId\": \"res-xyz789\", \"status\": \"reserved\"}"')

NOTIFICATIONS_ID=$(ensure_endpoint \
  "Notifications Send" "POST" "/api/notifications/send" \
  "Sends an order confirmation notification" \
  "Sent" 200 \
  '"{\"messageId\": \"msg-001\", \"status\": \"sent\"}"')

BLOCK_ID=$(ensure_block \
  "Order Flow" \
  "All external services called during order processing" \
  "#89b4fa" \
  "$PAYMENT_ID" "$INVENTORY_ID" "$NOTIFICATIONS_ID")

SUITE_ID=$(ensure_test_suite \
  "Order Flow Suite" \
  "Verifies all external services are called in the correct order" \
  "#cba6f7" \
  "$BLOCK_ID" "$PAYMENT_ID" "$INVENTORY_ID" "$NOTIFICATIONS_ID")

# Trigger
info ""
existing_trigger=$(find_trigger "Place Order")
if [ -n "$existing_trigger" ]; then
  info "Trigger \"Place Order\" already exists (id=$existing_trigger), skipping."
else
  info "Creating trigger: Place Order"
  ORDER_BODY='{"customerId":"cust-1","items":[{"sku":"BOOK-01","qty":2}],"totalAmount":29.99}'
  curl -sf -X POST "$BASE/triggers" \
    -H "Content-Type: application/json" \
    -d "{
      \"name\": \"Place Order\",
      \"description\": \"Posts a test order to the order service\",
      \"type\": \"HTTP\",
      \"httpMethod\": \"POST\",
      \"httpUrl\": \"http://localhost:3000/orders\",
      \"httpBody\": $(echo "$ORDER_BODY" | python3 -c 'import sys,json; print(json.dumps(sys.stdin.read().strip()))'),
      \"enabled\": true,
      \"testSuite\": {\"id\": $SUITE_ID}
    }" > /dev/null
  info "  -> done"
fi

info ""
info "Done! Workflow:"
info "  1. Run this script once to set up BlockMock"
info "  2. Open Test Suites → Start Run on 'Order Flow Suite'"
info "  3. Open Runs → click '▶ Place Order' to fire the trigger"
info "  4. Click '✓ Complete' — BlockMock evaluates the expectations"
