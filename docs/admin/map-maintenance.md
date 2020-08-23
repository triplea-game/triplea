# Map Maintenance

Notes for map admins for map maintenance:

Below assumes you'll clone: `triplea-game/assets` -> `~/work/assets`, and create a folder called `~/work/maps` into which all maps will be cloned.

***Clone all maps:***
```
cd ~/work/maps/

export OAUTH_TOKEN=............
for i in $(seq 1 6); do
  for map in $(
    curl -s -H "Authorization: token ${OAUTH_TOKEN}" \
        "https://api.github.com/orgs/triplea-maps/repos?page=$i&size=100" \
      | grep "ssh_url" \
      | sed 's|^.*: "\(.*\)",$|\1|' \
  ); do
    git clone $map
  done
done
```

***Find all Player Names***

```
cd maps/
echo "" > full_player_list;

while read i; do
   grep "<player name" "$i" \
      | sed 's/^.*name="//' \
      | sed 's/".*//'  >> full_player_list; 
done < <(find . -path "**/games/**" -name "*.xml");

sort full_player_list | uniq -c > players
mv players full_player_list
```

***Find flags that do not belong to any player***

```
cd maps/
for i in $(ls ../assets/game_headed_assets/flags/ | sed 's/.png$//' | sed 's/.gif$//' | sed 's/_small$//' | sed 's/_large//' | sort | uniq); do grep -q $i full_player_list || echo "$i not found"; done;
```

***List of all resources***

```
cd maps/
while read map; do grep "resource" "$map"; done < <(find . -name "*.xml")  | grep "resource name" | sed 's/^\s*//'  | sort | uniq
```

***List of all Territory Effects***
```
cd maps/
while read map; do grep "territoryEffect" "$map"; done < <(find . -name "*.xml")  | grep "territoryEffect name=" | sed 's/^\s*//' | sed 's/.*name="//' | sed 's/".*$//' | tr 'A-Z' 'a-z' | sort | uniq
```
