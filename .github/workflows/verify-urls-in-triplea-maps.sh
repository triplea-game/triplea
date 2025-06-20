#!/bin/bash
set -x
echo "Extracting URLs from triplea_maps.yaml..."
grep -Eo 'https://[^") >]+' triplea_maps.yaml | sort -u > triplea_maps_urls.txt || true

if [ ! -s triplea_maps_urls.txt ]; then
  echo "❌ No URLs found in triplea_maps.yaml."
  exit 1
fi

echo "Checking URLs..."
failed_urls=()

while IFS= read -r url; do
  curl --fail --silent "$url" > /dev/null || failed_urls+=("$url")
done < triplea_maps_urls.txt

if [ "${#failed_urls[@]}" -ne 0 ]; then
  echo "❌ The following URLs failed verification:"
  for url in "${failed_urls[@]}"; do
    echo "$url"
  done
  exit 1
else
  echo "✅ All URLs are valid."
fi
