#!/bin/bash

set -u

TOKEN_FILE="$(cd ~; pwd)/.github/token"

if [ ! -f "$TOKEN_FILE" ]; then
  echo "Error: missing token file at $TOKEN_FILE"
  echo "  This token file should contain a github personnal access token"
  exit 1 
fi

YAML_FILE="triplea_maps.yaml"
if [ ! -f "$YAML_FILE" ]; then
  echo "Error: could not find file 'triplea_maps.yaml' in the current folder."
  echo "Usage: cd ~/...../triplea; ./scripts/$(basename $0)"
  exit 2
fi

ACCESS_TOKEN=$(cat "$TOKEN_FILE")
GITHUB_AUTH="Authorization: token $ACCESS_TOKEN"

if [ -z "$ACCESS_TOKEN" ]; then
  echo "Failed to get an access token. Expected it to be in file $TOKEN_FILE"
  exit 3
fi



FOUND_COUNT=0
NOT_FOUND_COUNT=0
NOT_FOUND_LIST=""
LATEST_COUNT=0
LATEST_LIST=""
NOT_LATEST_COUNT=0

echo 
echo 
echo "-------------------------"
echo "version check" 
echo 

for j in 1 2;
do
  for i in $(curl --silent "https://api.github.com/orgs/triplea-maps/repos?page=$j&per_page=100" | grep git_url | sed 's/.*: "//i' | sed 's/",$//'); do
    #echo  "^- url:.*$(echo $i | sed 's/.*github.com//' | sed 's/.git//')" $YAML_FILE
   URL_MAP_NAME_PART=$(echo $i | sed 's/.*github.com//' | sed 's/.git$//')
   if [ "$(egrep "url:.*$URL_MAP_NAME_PART/" $YAML_FILE)" == "" ]; then
     NOT_FOUND_LIST=$(echo $NOT_FOUND_LIST $i | sed 's/git:.*triplea-maps//g')
     NOT_FOUND_COUNT=$((NOT_FOUND_COUNT+1))
   else
     MAP_NAME=$(echo $URL_MAP_NAME_PART | sed 's|.*/||')
     latestTag=$(curl -s -H "$GITHUB_AUTH" "https://api.github.com/repos/triplea-maps/$MAP_NAME/releases" | grep "\"tag_name\"" | sed 's/",$//' | sed 's/.*"//g' | head -1)
   
   
     CURRENT=$(egrep "^-? *url.*$URL_MAP_NAME_PART/releases/download/$latestTag" triplea_maps.yaml)
     if [ -z "$CURRENT" ]; then
       NOT_LATEST_COUNT=$((NOT_LATEST_COUNT+1))
       ACTUAL_CURRENT=$(egrep "^-? *url.*$URL_MAP_NAME_PART/releases/download" triplea_maps.yaml | sed 's|.*download/||g')
       echo "$MAP_NAME latest = ($latestTag) -> current = $ACTUAL_CURRENT"
     else 
       LATEST_COUNT=$((LATEST_COUNT+1))
       LATEST_LIST=$(echo $LATEST_LIST $MAP_NAME)
       echo "   is latest - $MAP_NAME"
     fi
    fi

  done;
done;
       

echo 
echo 
echo "-------------------------"
echo summary: 
echo 
echo "LATEST => $LATEST_COUNT"
echo "$LATEST_LIST"
echo 
echo "NOT_FOUND => $NOT_FOUND_COUNT"
echo "$NOT_FOUND_LIST"

echo "Not Latest: $NOT_LATEST_COUNT"
exit 


curl -s -H "$GITHUB_AUTH" "https://api.github.com/repos/triplea-maps/$mapFolder/releases" | grep -c \"tag_name\"
