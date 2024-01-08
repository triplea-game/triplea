#!/bin/bash

# Connect to a locally running database that was started up via docker-compose.

set -eu

DB_IP=$(docker inspect \
  --format='{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}' \
  triplea-database-1)

export PGPASSWORD=postgres
psql -h "$DB_IP" -U postgres
