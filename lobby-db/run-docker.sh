#!/bin/bash

set -eux
function stop_container() {
  local runningContainerId=$(docker container ls | grep triplea-lobby-db | cut -f 1 -d ' ')
  if [ ! -z "$runningContainerId" ]; then
     docker container stop $runningContainerId
     docker container rm $runningContainerId
  fi
}

function start_container() {
  docker run -d --name=triplea-lobby-db -p 5432:5432 triplea/lobby-db
}

stop_container
start_container
