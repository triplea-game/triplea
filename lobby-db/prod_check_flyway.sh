#!/bin/bash


set -eu

CREDS_FILE=~/lobby.db.creds

bold=$(tput bold)
normal=$(tput sgr0)
red='\033[0;31m'

if [ ! -f "$CREDS_FILE" ]; then
  echo -e "${bold}${red} Error:${normal}${bold} Creds file does not exist at: $CREDS_FILE${normal}"

  echo "user=" > $CREDS_FILE
  echo "pass=" >> $CREDS_FILE

  echo "${bold}I have created the file for you, it contains:${normal}"
  cat $CREDS_FILE

  echo ""
  echo "Please update the file with valid database username and password and re-run this script"
  exit 1
fi

# read 'user' and 'pass' property value from creds file
USER=$(grep user "$CREDS_FILE" | sed 's/.*=//')
PASS=$(grep pass "$CREDS_FILE" | sed 's/.*=//')

if [ -z "$USER" ] || [ -z "$PASS" ]; then
  echo "${bold}Failed to read user name and password properties from $CREDS_FILE${normal}"
  echo "Please update the file to have user and password"
  echo "You can delete the file to recreate it with placeholder usename and password properties."
  exit 1
fi

../gradlew -Pflyway.user="$USER" -Pflyway.password="$PASS" flywayInfo

