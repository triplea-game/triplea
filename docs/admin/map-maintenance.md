# Map Maintenance

Notes for map admins on managing the map maintenance:

- Create a 'maps' work folder: `mkdir -p ~/work/maps; cd ~/work/maps`
- Clone repo: <https://github.com/triplea-maps/admin_scripts>
- Run clone-all script: <https://github.com/triplea-maps/admin_scripts/blob/master/bin/clone_all.sh>

At the end of this, the `~/work/maps/` folder will contain a cloned copy of each of the github map repositories.
From here individual updates can be made and pushed, or changes can be made in bulk using 'for-each' loops. Check
the 'admin_scripts' repository for additional useful scripts and general patterns for bulk changes.

