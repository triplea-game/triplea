#!/bin/bash

set -e

scriptDir=$(realpath "$(dirname "$0")")
rootDir="$scriptDir/.."

"$rootDir/gradlew" composeUp
