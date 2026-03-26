#!/usr/bin/env bash
set -uo pipefail

BASE="${BLOCKMOCK_URL:-http://localhost:8080}/api"

info()  { echo "$@" >&2; }
ok()    { echo "  ✓ $*" >&2; }
fail()  { echo "  ✗ $*" >&2; exit 1; }

post() {
  local path="$1"
  curl -s -X POST "$BASE/$path" -H "Content-Type: application/json" -d @-
}

extract_id() {
  grep -o '"id":[0-9]*' | head -1 | cut -d: -f2 || true
}

find_by_name() {
  local resource="$1" name="$2"
  curl -s "$BASE/$resource" \
    | grep -o "\"id\":[0-9]*,\"name\":\"$name\"" \
    | grep -o '"id":[0-9]*' | cut -d: -f2 | head -1 || true
}

delete_if_exists() {
  local resource="$1" name="$2"
  local id
  id=$(find_by_name "$resource" "$name")
  if [ -n "$id" ]; then
    curl -s -X DELETE "$BASE/$resource/$id" > /dev/null
    info "  Deleted $resource \"$name\" (id=$id)"
  fi
}

# ── Reset ────────────────────────────────────────────────────────────────────

if [ "${1:-}" = "--reset" ]; then
  info "Resetting AMQP demo resources..."
  delete_if_exists "triggers"    "Send order.incoming"
  delete_if_exists "test-suites" "AMQP Order Suite"
  delete_if_exists "blocks"      "AMQP Order Flow"
  delete_if_exists "endpoints"   "Order Confirmed"
  info "Reset complete."
  exit 0
fi

# ── Setup ────────────────────────────────────────────────────────────────────

info "Setting up AMQP Order Suite in BlockMock..."
info ""

# 1. Endpoint: order.confirmed (BlockMock subscribes here)
info "1. Creating AMQP endpoint: order.confirmed (RECEIVE)..."
ENDPOINT_RESP=$(post "endpoints" <<'EOF'
{
  "name":        "Order Confirmed",
  "description": "BlockMock receives order.confirmed events from the order service",
  "protocol":    "AMQP",
  "pattern":     "FIRE_FORGET",
  "amqpAddress": "order.confirmed",
  "amqpPattern": "RECEIVE",
  "enabled":     true,
  "responses":   []
}
EOF
)
ENDPOINT_ID=$(echo "$ENDPOINT_RESP" | extract_id)
[ -n "$ENDPOINT_ID" ] || fail "Failed to create endpoint. Response: $ENDPOINT_RESP"
ok "Endpoint created (id=$ENDPOINT_ID)"

# 2. Block
info "2. Creating block: AMQP Order Flow..."
BLOCK_RESP=$(post "blocks" <<'EOF'
{
  "name":        "AMQP Order Flow",
  "description": "AMQP endpoints for the order flow demo",
  "color":       "#94e2d5"
}
EOF
)
BLOCK_ID=$(echo "$BLOCK_RESP" | extract_id)
[ -n "$BLOCK_ID" ] || fail "Failed to create block. Response: $BLOCK_RESP"
ok "Block created (id=$BLOCK_ID)"

# 3. Add endpoint to block
info "3. Adding endpoint to block..."
curl -s -X POST "$BASE/blocks/$BLOCK_ID/endpoints/$ENDPOINT_ID" > /dev/null
ok "Endpoint added to block"

# 4. Start the block (activates the AMQP consumer on order.confirmed)
info "4. Starting block (activating AMQP consumer)..."
curl -s -X POST "$BASE/blocks/$BLOCK_ID/start" > /dev/null
ok "Block started — BlockMock is now listening on order.confirmed"

# 5. Test suite
info "5. Creating test suite: AMQP Order Suite..."
SUITE_RESP=$(post "test-suites" <<EOF
{
  "name":        "AMQP Order Suite",
  "description": "Verifies the order service publishes correct AMQP events",
  "color":       "#94e2d5",
  "blocks":      [{"id": $BLOCK_ID}]
}
EOF
)
SUITE_ID=$(echo "$SUITE_RESP" | extract_id)
[ -n "$SUITE_ID" ] || fail "Failed to create test suite. Response: $SUITE_RESP"
ok "Test suite created (id=$SUITE_ID)"

# 6. Scenario
info "6. Creating scenario: Order Confirmed..."
SCENARIO_RESP=$(post "test-suites/$SUITE_ID/scenarios" <<EOF
{
  "name":        "Order Confirmed",
  "description": "BlockMock fires an order.incoming message; order service must publish order.confirmed",
  "expectations": [
    {
      "name":                 "order.confirmed event published",
      "mockEndpoint":         {"id": $ENDPOINT_ID, "protocol": "AMQP"},
      "minCallCount":         1,
      "maxCallCount":         1,
      "requiredBodyContains": "\"status\":\"confirmed\"",
      "expectationOrder":     null
    }
  ],
  "responseOverrides": []
}
EOF
)
info "  (scenario response: $SCENARIO_RESP)"
SCENARIO_ID=$(echo "$SCENARIO_RESP" | extract_id)
[ -n "$SCENARIO_ID" ] || fail "Failed to create scenario. Response: $SCENARIO_RESP"
ok "Scenario created (id=$SCENARIO_ID)"

# 7. AMQP trigger
info "7. Creating AMQP trigger: Send order.incoming..."
TRIGGER_RESP=$(post "triggers" <<EOF
{
  "name":         "Send order.incoming",
  "description":  "Publishes a test order to the order.incoming queue",
  "type":         "AMQP",
  "amqpAddress":  "order.incoming",
  "amqpBody":     "{\"customerId\":\"cust-amqp-1\",\"items\":[{\"sku\":\"WIDGET-01\",\"qty\":3}],\"totalAmount\":49.95}",
  "testScenario": {"id": $SCENARIO_ID},
  "enabled":      true
}
EOF
)
TRIGGER_ID=$(echo "$TRIGGER_RESP" | extract_id)
[ -n "$TRIGGER_ID" ] || fail "Failed to create trigger. Response: $TRIGGER_RESP"
ok "Trigger created (id=$TRIGGER_ID)"

# ── Done ─────────────────────────────────────────────────────────────────────

info ""
info "Done! Demo workflow:"
info ""
info "  Prerequisites:"
info "    docker-compose up -d    (Artemis on port 5672)"
info "    mvn quarkus:dev         (BlockMock on port 8080)"
info ""
info "  Start the order service:"
info "    cd demo/order-service-amqp && npm install && node app.js"
info "    — or —"
info "    docker-compose --profile demo up -d demo-order-service-amqp"
info ""
info "  Run the test:"
info "    1. Open BlockMock UI → Test Suites → 'AMQP Order Suite'"
info "    2. Click '▶ Run' on scenario 'Order Confirmed'"
info "    3. In the Runs modal → '▶ Trigger' (fires Send order.incoming)"
info "    4. Click '✓ Complete'"
info "    5. Expect: order.confirmed event published ✓"
info ""
info "  Reset:"
info "    ./setup-blockmock.sh --reset"
