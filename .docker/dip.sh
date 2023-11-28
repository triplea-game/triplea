#!/bin/bash

#
# dip - Displays an equivalent of 'docker container ls' but
#       also with IP addresses.
#
# Source this file, then can invoke command "dip"
#
#
# Example Output:
#
# NAMES                        ┊IP        ┊STATUS                 ┊SIZE
# triplea-database-1           ┊172.20.0.2┊Up 34 minutes (healthy)┊63B (virtual 202MB)
# triplea-game-support-server-1┊172.20.0.4┊Up 20 minutes          ┊32.8kB (virtual 241MB)
# triplea-lobby-1              ┊172.20.0.3┊Up 20 minutes          ┊32.8kB (virtual 244MB)
# triplea-nginx-1              ┊172.20.0.5┊Up 20 minutes          ┊2B (virtual 58.5MB)
#

docker_ps="$(
  docker ps --format 'table{{.Names}}|{{.Status}}|{{.Size}}' |
    tail --lines +2 |
    sort
)"

# https://stackoverflow.com/a/20686101
FORMAT='{{.Name}}|{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}'

ip_addresses=$(
  echo "$docker_ps" |
    cut --fields 1 --delimiter '|' |
    while read -r container; do
      docker inspect --format=$FORMAT "$container"
    done |
    sed 's|^/||' |
    sort
)

{
  FM="\033[1m%s\033[0m|\033[1m%s\033[0m|\033[1m%s\033[0m|\033[1m%s\033[0m"
  printf "$FM\n" NAMES IP STATUS SIZE
  join -t "|" <(echo "$ip_addresses") <(echo "$docker_ps")
} |
  column --separator "|" --output-separator "┊" --table
