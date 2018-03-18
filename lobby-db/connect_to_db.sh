#!/bin/bash

PSQL="sudo -u postgres psql postgres"

if [[ "$OSTYPE" == darwin* ]]; then
  PSQL="psql -h localhost -U postgres"
fi

$PSQL

