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

find_scenario() {
  local suite_id="$1" name="$2"
  curl -sf "$BASE/test-suites/$suite_id/scenarios" | grep -o "\"id\":[0-9]*,\"name\":\"$name\"" | grep -o '"id":[0-9]*' | cut -d: -f2
}

find_trigger() {
  local name="$1"
  curl -sf "$BASE/triggers" | grep -o "\"id\":[0-9]*,\"name\":\"$name\"" | grep -o '"id":[0-9]*' | cut -d: -f2
}

find_response() {
  local endpoint_id="$1" name="$2"
  curl -sf "$BASE/endpoints/$endpoint_id" | grep -o "\"id\":[0-9]*,\"name\":\"$name\"" | grep -o '"id":[0-9]*' | cut -d: -f2
}

ensure_response() {
  local endpoint_id="$1" name="$2" status="$3" body="$4"

  info ""
  local existing
  existing=$(find_response "$endpoint_id" "$name")
  if [ -n "$existing" ]; then
    info "Response \"$name\" already exists on endpoint $endpoint_id (id=$existing), skipping."
    echo "$existing"
    return
  fi

  info "Adding response \"$name\" ($status) to endpoint $endpoint_id"
  local RESP RESP_ID
  RESP=$(curl -sf -X POST "$BASE/endpoints/$endpoint_id/responses" \
    -H "Content-Type: application/json" \
    -d "{
      \"name\": \"$name\",
      \"priority\": 0,
      \"responseStatusCode\": $status,
      \"responseBody\": $body,
      \"responseDelayMs\": 0
    }")
  RESP_ID=$(echo "$RESP" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
  info "  -> id=$RESP_ID"
  echo "$RESP_ID"
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

  info ""
  local existing
  existing=$(find_test_suite "$name")
  if [ -n "$existing" ]; then
    info "Test suite \"$name\" already exists (id=$existing), skipping."
    echo "$existing"
    return
  fi

  info "Creating test suite: $name"
  local SUITE SUITE_ID
  SUITE=$(curl -sf -X POST "$BASE/test-suites" \
    -H "Content-Type: application/json" \
    -d "{
      \"name\": \"$name\",
      \"description\": \"$description\",
      \"color\": \"$color\",
      \"blocks\": [{\"id\": $block_id}]
    }")
  SUITE_ID=$(echo "$SUITE" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
  info "  -> id=$SUITE_ID"
  echo "$SUITE_ID"
}

ensure_scenario() {
  local suite_id="$1" name="$2" description="$3"
  local payment_id="$4" inventory_id="$5" notifications_id="$6"

  info ""
  local existing
  existing=$(find_scenario "$suite_id" "$name")
  if [ -n "$existing" ]; then
    info "Scenario \"$name\" already exists (id=$existing), skipping."
    echo "$existing"
    return
  fi

  info "Creating scenario: $name"
  local SCENARIO SCENARIO_ID
  SCENARIO=$(curl -sf -X POST "$BASE/test-suites/$suite_id/scenarios" \
    -H "Content-Type: application/json" \
    -d "{
      \"name\": \"$name\",
      \"description\": \"$description\",
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
  SCENARIO_ID=$(echo "$SCENARIO" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
  info "  -> id=$SCENARIO_ID"
  info "  -> 3 expectations (payment → inventory → notification, sequence enforced)"
  echo "$SCENARIO_ID"
}

ensure_payment_failure_scenario() {
  local suite_id="$1" payment_id="$2" payment_failure_response_id="$3"
  local inventory_id="$4" notifications_id="$5"

  local name="Payment Failure"
  info ""
  local existing
  existing=$(find_scenario "$suite_id" "$name")
  if [ -n "$existing" ]; then
    info "Scenario \"$name\" already exists (id=$existing), skipping."
    echo "$existing"
    return
  fi

  info "Creating scenario: $name"
  local SCENARIO SCENARIO_ID
  SCENARIO=$(curl -sf -X POST "$BASE/test-suites/$suite_id/scenarios" \
    -H "Content-Type: application/json" \
    -d "{
      \"name\": \"$name\",
      \"description\": \"Payment service returns 402 — inventory and notifications must NOT be called\",
      \"expectations\": [
        {
          \"name\": \"Payment attempted\",
          \"mockEndpoint\": {\"id\": $payment_id},
          \"minCallCount\": 1,
          \"maxCallCount\": 1
        },
        {
          \"name\": \"Inventory NOT reserved\",
          \"mockEndpoint\": {\"id\": $inventory_id},
          \"minCallCount\": 0,
          \"maxCallCount\": 0
        },
        {
          \"name\": \"Notification NOT sent\",
          \"mockEndpoint\": {\"id\": $notifications_id},
          \"minCallCount\": 0,
          \"maxCallCount\": 0
        }
      ],
      \"responseOverrides\": [
        {
          \"mockEndpoint\": {\"id\": $payment_id},
          \"mockResponse\": {\"id\": $payment_failure_response_id}
        }
      ]
    }")
  SCENARIO_ID=$(echo "$SCENARIO" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
  info "  -> id=$SCENARIO_ID"
  info "  -> payment forced to 402, inventory+notification expected 0x"
  echo "$SCENARIO_ID"
}

delete_by_name() {
  local resource="$1" name="$2" find_fn="$3"
  local id
  id=$($find_fn "$name")
  if [ -n "$id" ]; then
    curl -sf -X DELETE "$BASE/$resource/$id" > /dev/null
    info "  Deleted $resource \"$name\" (id=$id)"
  fi
}

reset_demo() {
  info "Resetting demo resources..."
  delete_by_name "triggers" "Place Order (Failure)" find_trigger
  delete_by_name "triggers" "Place Order"           find_trigger
  delete_by_name "test-suites" "Order Flow Suite"   find_test_suite
  delete_by_name "blocks" "Order Flow"              find_block
  delete_by_name "endpoints" "Payment Charge"       find_endpoint
  delete_by_name "endpoints" "Inventory Reserve"    find_endpoint
  delete_by_name "endpoints" "Notifications Send"   find_endpoint
  info "Reset complete."
  info ""
}

if [ "${1:-}" = "--reset" ]; then
  reset_demo
fi

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
  "$BLOCK_ID")

SCENARIO_ID=$(ensure_scenario \
  "$SUITE_ID" \
  "Happy Path" \
  "All services called once in the correct order" \
  "$PAYMENT_ID" "$INVENTORY_ID" "$NOTIFICATIONS_ID")

PAYMENT_DECLINED_RESPONSE_ID=$(ensure_response \
  "$PAYMENT_ID" \
  "Payment Declined" \
  402 \
  '"{\"error\": \"payment_declined\", \"message\": \"Insufficient funds\"}"')

PAYMENT_FAILURE_SCENARIO_ID=$(ensure_payment_failure_scenario \
  "$SUITE_ID" "$PAYMENT_ID" "$PAYMENT_DECLINED_RESPONSE_ID" \
  "$INVENTORY_ID" "$NOTIFICATIONS_ID")

ORDER_BODY='{"customerId":"cust-1","items":[{"sku":"BOOK-01","qty":2}],"totalAmount":29.99}'

# Trigger for Happy Path
info ""
existing_trigger=$(find_trigger "Place Order")
if [ -n "$existing_trigger" ]; then
  info "Trigger \"Place Order\" already exists (id=$existing_trigger), skipping."
else
  info "Creating trigger: Place Order"
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
      \"testScenario\": {\"id\": $SCENARIO_ID}
    }" > /dev/null
  info "  -> done"
fi

# Trigger for Payment Failure (same request body — the override makes payment return 402)
info ""
existing_pf_trigger=$(find_trigger "Place Order (Failure)")
if [ -n "$existing_pf_trigger" ]; then
  info "Trigger \"Place Order (Failure)\" already exists (id=$existing_pf_trigger), skipping."
else
  info "Creating trigger: Place Order (Failure)"
  curl -sf -X POST "$BASE/triggers" \
    -H "Content-Type: application/json" \
    -d "{
      \"name\": \"Place Order (Failure)\",
      \"description\": \"Same order request — payment mock is forced to 402 by the scenario override\",
      \"type\": \"HTTP\",
      \"httpMethod\": \"POST\",
      \"httpUrl\": \"http://localhost:3000/orders\",
      \"httpBody\": $(echo "$ORDER_BODY" | python3 -c 'import sys,json; print(json.dumps(sys.stdin.read().strip()))'),
      \"enabled\": true,
      \"testScenario\": {\"id\": $PAYMENT_FAILURE_SCENARIO_ID}
    }" > /dev/null
  info "  -> done"
fi

info ""
info "Done! Workflow:"
info "  1. Run this script once to set up BlockMock"
info ""
info "  Happy Path:"
info "    a. Test Suites → 'Order Flow Suite' → Happy Path → '▶ Run'"
info "    b. Runs modal → '▶ Place Order' → '✓ Complete'"
info "    c. Expect: payment ✓  inventory ✓  notification ✓  (in order)"
info ""
info "  Payment Failure:"
info "    a. Test Suites → 'Order Flow Suite' → Payment Failure → '▶ Run'"
info "    b. Runs modal → '▶ Place Order (Failure)' → '✓ Complete'"
info "    c. Expect: payment ✓  inventory 0x ✓  notification 0x ✓"
info "    (payment mock is forced to 402 — order service must abort early)"
