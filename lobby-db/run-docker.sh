#!/bin/bash

docker run -d --name=triplea-lobby-db -p 5432:5432 triplea/lobby-db
