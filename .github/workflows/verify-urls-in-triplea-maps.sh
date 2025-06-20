#!/bin/bash

failed_urls=()
for url in $(grep -Eo 'https://[^") >]+' triplea_maps.yaml | sort -u); do
  curl --fail --silent "$url" > /dev/null || failed_urls+=("$url")
done

if [ "${#failed_urls[@]}" -ne 0 ]; then
  echo "❌ The following URLs failed verification:"
  for url in "${failed_urls[@]}"; do
    echo "$url"
  done
  exit 1
else
  echo "✅ All URLs are valid."
fi
