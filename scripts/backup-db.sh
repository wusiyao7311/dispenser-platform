#!/usr/bin/env bash
#
# Nightly pg_dump backup with retention cleanup. Intended to run from cron,
# e.g.:  0 2 * * *  /opt/dispenser-platform/scripts/backup-db.sh
#
# Required env vars: DB_HOST, DB_NAME, DB_USER, PGPASSWORD
# Optional: BACKUP_DIR (default /var/backups/dispenser-platform), RETENTION_DAYS (default 14)

set -euo pipefail

: "${DB_HOST:?DB_HOST is required}"
: "${DB_NAME:?DB_NAME is required}"
: "${DB_USER:?DB_USER is required}"
: "${PGPASSWORD:?PGPASSWORD is required}"

BACKUP_DIR="${BACKUP_DIR:-/var/backups/dispenser-platform}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"
TIMESTAMP=$(date -u +'%Y%m%dT%H%M%SZ')
OUT_FILE="${BACKUP_DIR}/dispensers-${TIMESTAMP}.sql.gz"

mkdir -p "$BACKUP_DIR"

echo "Backing up ${DB_NAME}@${DB_HOST} to ${OUT_FILE}"
pg_dump -h "$DB_HOST" -U "$DB_USER" -d "$DB_NAME" --no-owner | gzip > "$OUT_FILE"

echo "Pruning backups older than ${RETENTION_DAYS} days"
find "$BACKUP_DIR" -name 'dispensers-*.sql.gz' -mtime "+${RETENTION_DAYS}" -delete

echo "Done: $(du -h "$OUT_FILE" | cut -f1) written"
