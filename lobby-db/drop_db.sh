#!/bin/bash


if [[ "$OSTYPE" == darwin* ]]; then
  PSQL="psql -h localhost -U postgres"
fi
echo "Using: $PSQL"
echo "drop database ta_users" | $PSQL


