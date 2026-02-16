#!/bin/bash

# Browser-like user agent (Chrome on Linux)
USER_AGENT="Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Safari/537.36"

# URLs that must always be checked even if not in YAML
WHITELIST_URLS=(
  "https://www.axisandallies.org/forums/topic/33233/argo-s-middleweight-map-for-1939-1942" # PR #14127
)

is_whitelisted() {
  local url="$1"
  for w in "${WHITELIST_URLS[@]}"; do
    [[ "$url" == "$w" ]] && return 0
  done
  return 1
}

failed_urls=()
for url in $(grep -Eo 'https://[^") >]+' triplea_maps.yaml | sort -u); do
  # 1) Try normal curl first
  if curl --fail --silent "$url" > /dev/null; then
    continue
  fi
  

  # 2) If failed AND whitelisted → retry with browser user agent
  if is_whitelisted "$url"; then
    echo "Retrying with browser UA: $url"
    curl --fail --silent -A "$USER_AGENT" "$url" > /dev/null && continue
  fi

  # 3) If still failing → mark as failed
  failed_urls+=("$url")
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
