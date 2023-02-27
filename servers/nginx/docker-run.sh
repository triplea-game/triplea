#!/bin/bash

docker run \
  --rm -d \
  --name="nginx" \
  --network host \
  "nginx:stable-alpine-perl"

