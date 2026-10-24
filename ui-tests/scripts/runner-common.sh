#!/usr/bin/env bash

# Sourced by platform runners. These helpers need no device/build.
initialize_maestro_run() {
  UI_TEST_RUN_ID=${UI_TEST_RUN_ID:-$(date -u +%Y%m%dT%H%M%S)-$$}
  MAESTRO_FLOW_ATTEMPTS=${MAESTRO_FLOW_ATTEMPTS:-2}
  MAESTRO_FLOW_TIMEOUT=${MAESTRO_FLOW_TIMEOUT:-8m}
  if [[ ! "$MAESTRO_FLOW_ATTEMPTS" =~ ^[1-9][0-9]*$ ]]; then
    echo "MAESTRO_FLOW_ATTEMPTS must be a positive integer" >&2
    return 1
  fi
  export UI_TEST_RUN_ID MAESTRO_FLOW_ATTEMPTS MAESTRO_FLOW_TIMEOUT MAESTRO_OUTPUT_DIR
}

run_maestro_attempt() {
  local suite_name="$1"
  shift
  python3 "$ROOT_DIR/ui-tests/scripts/run_maestro.py" \
    --output-dir "$MAESTRO_OUTPUT_DIR" --suite "$suite_name" \
    --timeout "$MAESTRO_FLOW_TIMEOUT" -- "$@"
}

# Optional local smoke selection; never changes the default CI inventory.
flow_is_selected() {
  [[ -z "${MAESTRO_FLOW_FILTER:-}" || "$1" =~ $MAESTRO_FLOW_FILTER ]]
}

run_bounded() {
  python3 "$ROOT_DIR/ui-tests/scripts/run_bounded.py" "$@"
}

wait_for_android_boot() {
  local adb_bin="$1"
  local serial="$2"
  local deadline=$((SECONDS + 180))
  while ((SECONDS < deadline)); do
    if [[ "$(run_bounded 10s "$adb_bin" -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '[:space:]')" == "1" ]]; then
      return 0
    fi
    sleep 2
  done
  printf 'Android emulator %s did not boot within 180s\n' "$serial" >&2
  return 1
}

record_flow_failure() {
  python3 "$ROOT_DIR/ui-tests/scripts/run_maestro.py" \
    --output-dir "$MAESTRO_OUTPUT_DIR" --suite "$1" --error "$2"
}

validate_shard() {
  case "$1" in
    login | free | free-a | free-b | premium | all) ;;
    *)
      printf 'Unknown shard %q (expected: login, free, free-a, free-b, premium, all)\n' "$1" >&2
      return 1
      ;;
  esac
}

# Preserve the exact local config, not a possibly different release config.
use_test_service_file() {
  SERVICE_FILE="$1"
  local test_file="$2"
  BACKUP_SERVICE_FILE=""
  [[ -f "$test_file" ]] || return 1
  if [[ -e "$SERVICE_FILE" ]]; then
    # Xcode's synchronized source groups copy adjacent unknown files into the app.
    # Keep the original configuration outside app resources, not beside the plist.
    BACKUP_SERVICE_FILE=$(mktemp "${TMPDIR:-/tmp}/budgetplus-service.XXXXXX") || return 1
    if ! cp -p "$SERVICE_FILE" "$BACKUP_SERVICE_FILE"; then
      rm -f "$BACKUP_SERVICE_FILE"
      return 1
    fi
  fi
  trap restore_service_file EXIT
  cp "$test_file" "$SERVICE_FILE"
}

restore_service_file() {
  local result=$?
  trap - EXIT
  if [[ -n "$BACKUP_SERVICE_FILE" ]]; then
    if cp -p "$BACKUP_SERVICE_FILE" "$SERVICE_FILE"; then
      rm -f "$BACKUP_SERVICE_FILE" || result=1
    else
      printf 'Failed to restore %s; original retained at %s\n' "$SERVICE_FILE" "$BACKUP_SERVICE_FILE" >&2
      result=1
    fi
  else
    # Restore absence too, rather than leaving a test config behind on CI.
    rm -f "$SERVICE_FILE" || result=1
  fi
  exit "$result"
}
