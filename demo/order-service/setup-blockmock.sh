#!/usr/bin/env bash
set -euo pipefail

BASE="${BLOCKMOCK_URL:-http://localhost:8080}/api"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

info() { echo "$@" >&2; }

find_test_suite() {
  local name="$1"
  curl -sf "$BASE/test-suites" | grep -o "\"id\":[0-9]*,\"name\":\"$name\"" | grep -o '"id":[0-9]*' | cut -d: -f2
}

find_trigger() {
  local name="$1"
  curl -sf "$BASE/triggers" | grep -o "\"id\":[0-9]*,\"name\":\"$name\"" | grep -o '"id":[0-9]*' | cut -d: -f2
}

find_block() {
  local name="$1"
  curl -sf "$BASE/blocks" | grep -o "\"id\":[0-9]*,\"name\":\"$name\"" | grep -o '"id":[0-9]*' | cut -d: -f2
}

find_endpoint() {
  local name="$1"
  curl -sf "$BASE/endpoints" | grep -o "\"id\":[0-9]*,\"name\":\"$name\"" | grep -o '"id":[0-9]*' | cut -d: -f2
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
  delete_by_name "triggers"    "Place Order (Failure)" find_trigger
  delete_by_name "triggers"    "Place Order"           find_trigger
  delete_by_name "test-suites" "Order Flow Suite"      find_test_suite
  delete_by_name "blocks"      "Order Flow"            find_block
  delete_by_name "endpoints"   "Payment Charge"        find_endpoint
  delete_by_name "endpoints"   "Inventory Reserve"     find_endpoint
  delete_by_name "endpoints"   "Notifications Send"    find_endpoint
  info "Reset complete."
  info ""
}

if [ "${1:-}" = "--reset" ]; then
  reset_demo
fi

info "Importing Order Flow Suite from order-flow-suite.json..."
RESULT=$(curl -sf -X POST "$BASE/import-export/suites" \
  -H "Content-Type: application/json" \
  -d @"$SCRIPT_DIR/order-flow-suite.json")

info "Import result: $RESULT"
info ""
info "Done! Workflow:"
info "  1. Run this script once to set up BlockMock"
info ""
info "  Happy Path:"
info "    a. Test Suites → 'Order Flow Suite' → Happy Path → '▶ Run'"
info "    b. Runs modal → '▶ Trigger' → '✓ Complete'"
info "    c. Expect: payment ✓  inventory ✓  notification ✓  (in order)"
info ""
info "  Payment Failure:"
info "    a. Test Suites → 'Order Flow Suite' → Payment Failure → '▶ Run'"
info "    b. Runs modal → '▶ Trigger' → '✓ Complete'"
info "    c. Expect: payment ✓  inventory 0x ✓  notification 0x ✓"
info "    (payment mock is forced to 402 — order service must abort early)"
