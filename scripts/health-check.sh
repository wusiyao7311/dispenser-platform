#!/usr/bin/env bash
#
# Polls the dispenser-platform health endpoint and low-stock report, and
# exits non-zero on failure so it can be wired into cron + alerting (e.g.
# a mail/Slack hook) for a recurring ops check.
#
# Usage: ./health-check.sh [base_url]
#   ./health-check.sh                        # defaults to http://localhost:8080
#   ./health-check.sh https://dispensers.example.com
#
# Email notifications are enabled when EMAIL_TO is set. Optional settings:
#   EMAIL_TO=ops@example.com EMAIL_FROM=monitor@example.com ./health-check.sh
#   EMAIL_SUBJECT_PREFIX='Dispenser platform' ./health-check.sh

set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
LOG_TAG="[health-check]"
EMAIL_TO="${EMAIL_TO:-}"
EMAIL_FROM="${EMAIL_FROM:-}"
EMAIL_SUBJECT_PREFIX="${EMAIL_SUBJECT_PREFIX:-Dispenser platform}"

log() {
    echo "$(date -u +'%Y-%m-%dT%H:%M:%SZ') ${LOG_TAG} $*"
}

notify_email() {
    local subject="$1"
    local body="$2"

    if [[ -z "$EMAIL_TO" ]]; then
        return 0
    fi

    if ! command -v mail >/dev/null 2>&1; then
        log "WARNING: EMAIL_TO is set but the mail command is unavailable"
        return 0
    fi

    if [[ -n "$EMAIL_FROM" ]]; then
        printf '%s\n' "$body" | mail -r "$EMAIL_FROM" -s "${EMAIL_SUBJECT_PREFIX}: ${subject}" "$EMAIL_TO"
    else
        printf '%s\n' "$body" | mail -s "${EMAIL_SUBJECT_PREFIX}: ${subject}" "$EMAIL_TO"
    fi
}

log "checking ${BASE_URL}/actuator/health"

if ! response=$(curl -fsS --max-time 5 "${BASE_URL}/actuator/health"); then
    log "ERROR: health endpoint unreachable"
    notify_email "Health check failed" "${BASE_URL}/actuator/health is unreachable."
    exit 1
fi

status=$(echo "$response" | grep -o '"status":"[A-Z]*"' | cut -d'"' -f4)

if [[ "$status" != "UP" ]]; then
    log "ERROR: reported status is '${status}', expected UP"
    notify_email "Health check failed" "${BASE_URL}/actuator/health reported status '${status}', expected UP."
    exit 1
fi

log "OK: application is UP"

low_stock_count=$(curl -fsS --max-time 5 "${BASE_URL}/api/dispensers/low-stock" | grep -o '"id"' | wc -l | tr -d ' ')

if [[ "$low_stock_count" -gt 0 ]]; then
    log "WARNING: ${low_stock_count} stock level(s) at or below threshold — restock needed"
    notify_email "Low stock alert" "${BASE_URL} reports ${low_stock_count} stock level(s) at or below threshold. Restock needed."
else
    log "OK: no low-stock dispensers"
fi

exit 0
