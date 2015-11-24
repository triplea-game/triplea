#!/bin/bash

## Verifies that maps listed in the maps.xml file are actually available for download

MAP_FILE="triplea_maps.xml"

if [ ! -f "$MAP_FILE" ]; then
  echo "Error: Could not find file: $MAP_FILE"
  echo "exiting"
  exit -1
fi

TOTAL=0
FAIL_COUNT=0

for i in $(grep "<url>" "$MAP_FILE"  | grep -v "<\!--" | sed 's| *</*url>||g' | grep -v "^\!" | sort | uniq);  do
  curl -sI "$i" > /dev/null 2>&1 || { echo "$i" is dead; FAIL_COUNT=$((FAIL_COUNT+1)); }
  TOTAL=$((TOTAL+1))
done;

echo "$FAIL_COUNT failed out of $TOTAL"
