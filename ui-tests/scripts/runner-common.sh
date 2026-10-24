#!/usr/bin/env bash

# Sourced by platform runners. These helpers need no device/build.
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
    BACKUP_SERVICE_FILE=$(mktemp "${SERVICE_FILE}.ui-test.XXXXXX") || return 1
    cp -p "$SERVICE_FILE" "$BACKUP_SERVICE_FILE" || return 1
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
