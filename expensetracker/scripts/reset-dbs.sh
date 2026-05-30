#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/docker-compose.yml"
PROJECT_NAME="${COMPOSE_PROJECT_NAME:-$(basename "$ROOT_DIR")}"
DB_VOLUMES=(
  pg_auth
  pg_user
  pg_group
  pg_expense
  pg_settlement
  pg_notification
  pg_analytics
)

YES=false
START=true
BUILD=false

usage() {
  cat <<'EOF'
Reset all ExpenseTracker PostgreSQL databases to their initial Flyway state.

Usage:
  scripts/reset-dbs.sh [--yes] [--no-start] [--build]

Options:
  --yes       Do not prompt before deleting database volumes.
  --no-start  Stop the stack and remove DB volumes, but do not start it again.
  --build     Rebuild service images when starting the stack again.
  --help      Show this help.

This removes the Docker Compose database volumes for the active project:
  pg_auth pg_user pg_group pg_expense pg_settlement pg_notification pg_analytics
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --yes|-y)
      YES=true
      ;;
    --no-start)
      START=false
      ;;
    --build)
      BUILD=true
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
  shift
done

cd "$ROOT_DIR"

if ! docker compose version >/dev/null 2>&1; then
  echo "docker compose is required but was not found." >&2
  exit 1
fi

if [[ "$YES" != true ]]; then
  echo "This will stop the ExpenseTracker compose stack and delete all Postgres database volumes."
  echo "Application data in authdb, userdb, groupdb, expensedb, settlementdb, notificationdb, and analyticsdb will be lost."
  read -r -p "Type RESET to continue: " answer
  if [[ "$answer" != "RESET" ]]; then
    echo "Aborted."
    exit 0
  fi
fi

echo "Stopping stack and removing database volumes..."
docker compose --project-directory "$ROOT_DIR" -f "$COMPOSE_FILE" down --remove-orphans

for volume in "${DB_VOLUMES[@]}"; do
  docker volume rm "${PROJECT_NAME}_${volume}" >/dev/null 2>&1 || true
done

if [[ "$START" == true ]]; then
  echo "Starting stack so services recreate schemas and seed data..."
  if [[ "$BUILD" == true ]]; then
    docker compose --project-directory "$ROOT_DIR" -f "$COMPOSE_FILE" up -d --build
  else
    docker compose --project-directory "$ROOT_DIR" -f "$COMPOSE_FILE" up -d
  fi
  echo "Done. Wait for services to finish startup migrations before using the app."
else
  echo "Done. Start later with: docker compose up -d"
fi
