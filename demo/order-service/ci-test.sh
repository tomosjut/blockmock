#!/usr/bin/env bash
set -euo pipefail

# Usage: ci-test.sh --suite NAME --scenario NAME [options]
#
# Options:
#   --suite NAME       Test suite name (required)
#   --scenario NAME    Scenario name (required)
#   --wait SECONDS     Seconds to wait after firing trigger (default: 3)
#   --junit FILE       Write JUnit XML to file
#   --url URL          BlockMock base URL (default: http://localhost:8080)

BASE="${BLOCKMOCK_URL:-http://localhost:8080}/api"
SUITE_NAME=""
SCENARIO_NAME=""
WAIT_SECONDS=3
JUNIT_FILE=""

usage() {
  echo "Usage: $0 --suite NAME --scenario NAME [--wait SECONDS] [--junit FILE] [--url URL]" >&2
  exit 1
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --suite)    SUITE_NAME="$2";   shift 2 ;;
    --scenario) SCENARIO_NAME="$2"; shift 2 ;;
    --wait)     WAIT_SECONDS="$2"; shift 2 ;;
    --junit)    JUNIT_FILE="$2";   shift 2 ;;
    --url)      BASE="${2%/}/api"; shift 2 ;;
    *)          echo "Unknown option: $1" >&2; usage ;;
  esac
done

[ -z "$SUITE_NAME" ] || [ -z "$SCENARIO_NAME" ] && usage

log()  { echo "[ci-test] $*" >&2; }
fail() { log "FATAL: $*"; exit 1; }

# ── Lookup ──────────────────────────────────────────────────────────────────

SUITE_ID=$(curl -sf "$BASE/test-suites" \
  | grep -o "\"id\":[0-9]*,\"name\":\"$SUITE_NAME\"" \
  | grep -o '"id":[0-9]*' | cut -d: -f2)
[ -z "$SUITE_ID" ] && fail "Suite not found: '$SUITE_NAME'"
log "Suite    : $SUITE_NAME (id=$SUITE_ID)"

SCENARIO_ID=$(curl -sf "$BASE/test-suites/$SUITE_ID/scenarios" \
  | grep -o "\"id\":[0-9]*,\"name\":\"$SCENARIO_NAME\"" \
  | grep -o '"id":[0-9]*' | cut -d: -f2)
[ -z "$SCENARIO_ID" ] && fail "Scenario not found: '$SCENARIO_NAME'"
log "Scenario : $SCENARIO_NAME (id=$SCENARIO_ID)"

TRIGGER_ID=$(curl -sf "$BASE/triggers" | python3 -c "
import sys, json
for t in json.load(sys.stdin):
    s = t.get('testScenario') or {}
    if t.get('enabled') and s.get('id') == $SCENARIO_ID:
        print(t['id']); break
")
[ -z "$TRIGGER_ID" ] && fail "No enabled trigger found for scenario '$SCENARIO_NAME'"
log "Trigger  : id=$TRIGGER_ID"

# ── Run ──────────────────────────────────────────────────────────────────────

log "Starting run..."
RUN=$(curl -sf -X POST "$BASE/test-suites/$SUITE_ID/scenarios/$SCENARIO_ID/runs")
RUN_ID=$(echo "$RUN" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
[ -z "$RUN_ID" ] && fail "Failed to start run"
log "Run started (id=$RUN_ID)"

log "Firing trigger..."
FIRE=$(curl -sf -X POST "$BASE/triggers/$TRIGGER_ID/fire")
HTTP_STATUS=$(echo "$FIRE" | grep -o '"responseStatus":[0-9]*' | cut -d: -f2)
[ -n "$HTTP_STATUS" ] && log "Trigger HTTP response: $HTTP_STATUS" || log "Trigger fired"

log "Waiting ${WAIT_SECONDS}s..."
sleep "$WAIT_SECONDS"

log "Completing run..."
RESULT=$(curl -sf -X POST \
  "$BASE/test-suites/$SUITE_ID/scenarios/$SCENARIO_ID/runs/$RUN_ID/complete")

# ── Results ──────────────────────────────────────────────────────────────────

echo "$RESULT" | python3 -c "
import sys, json
suite, scenario = '$SUITE_NAME', '$SCENARIO_NAME'
data = json.loads(sys.stdin.read())
status  = data.get('status', '?')
results = data.get('results', [])
passed  = sum(1 for r in results if r['passed'])
total   = len(results)
print(f'\n  {suite} / {scenario}  [{status}]', file=sys.stderr)
for r in results:
    icon   = '✓' if r['passed'] else '✗'
    count  = r.get('actualCallCount', 0)
    calls  = f\"{count} call{'s' if count != 1 else ''}\"
    reason = f\"  — {r['failureReason']}\" if r.get('failureReason') else ''
    print(f'  {icon}  expectation #{r[\"testExpectation\"][\"id\"]}:  {calls}{reason}', file=sys.stderr)
print(f'\n  {passed}/{total} passed\n', file=sys.stderr)
"

RUN_STATUS=$(echo "$RESULT" | grep -o '"status":"[A-Z]*"' | head -1 | cut -d'"' -f4)

# ── JUnit XML ────────────────────────────────────────────────────────────────

if [ -n "$JUNIT_FILE" ]; then
  curl -sf "$BASE/test-suites/$SUITE_ID/scenarios/$SCENARIO_ID/runs/$RUN_ID/junit" \
    > "$JUNIT_FILE"
  log "JUnit XML: $JUNIT_FILE"
fi

# ── Exit ─────────────────────────────────────────────────────────────────────

if [ "$RUN_STATUS" = "COMPLETED" ]; then
  log "PASSED"
  exit 0
else
  log "FAILED (status: $RUN_STATUS)"
  exit 1
fi
