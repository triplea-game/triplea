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




for j in 1 2;
do
  for i in $(curl --silent "https://api.github.com/orgs/triplea-maps/repos?page=$j&per_page=100" | grep git_url | sed 's/.*: "//i' | sed 's/",$//'); do
   URL_MAP_NAME_PART=$(echo $i | sed 's/.*github.com//' | sed 's/.git$//')
   
   if [ "$(egrep "^- url:.*$URL_MAP_NAME_PART/" $YAML_FILE)" != "" ]; then
     MAP_NAME=$(echo $URL_MAP_NAME_PART | sed 's|.*/||')
     latestTag=$(curl -s -H "$GITHUB_AUTH" "https://api.github.com/repos/triplea-maps/$MAP_NAME/releases" | grep "\"tag_name\"" | sed 's/",$//' | sed 's/.*"//g' | head -1)
   
     CURRENT=$(egrep "^-? *url.*$URL_MAP_NAME_PART/releases/download/$latestTag" triplea_maps.yaml)
     if [ -z "$CURRENT" ]; then
       echo "$MAP_NAME latest = ($latestTag)"
       
       sed -i "s|\($URL_MAP_NAME_PART/releases/download/\).*/|\1$latestTag/|" triplea_maps.yaml
       CURRENT=$(egrep "^-? *url.*$URL_MAP_NAME_PART/releases/download/" triplea_maps.yaml)
       echo "Now at: $CURRENT"
     fi
    fi

  done;
done;
       

