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

for i in $(grep "<url>" "$MAP_FILE"  | grep -v "<\!--" | sed 's| *</*url>||g' | grep -v "^\!" | sort | uniq | sed 's///g');  do
    # curl each URL with an HTTP header request and check if we get a 404
  curl -I "$i" 2>&1 | grep -q "302 Found" || {
   echo "FAILED $i"
   FAIL_COUNT=$((FAIL_COUNT+1))
  }
  TOTAL=$((TOTAL+1))
done;

echo "$FAIL_COUNT failed out of $TOTAL"
