#!/bin/bash

bold="\e[1m"
bold_green="${bold}\e[32m"
normal="\e[0m"

echo -e "[${bold_green}Inserting sample data${normal}]"

export PGPASSWORD=postgres
psql -h localhost -U postgres lobby_db \
    < "$(dirname "$0")/sql/sample_data/lobby_db_sample_data.sql"
