# Bot Ops

## Access

Done via SSH keys deployed via ansible.
```bash
ssh admin@<bot-dns-address>
```

## Logs

last 100 lines
```bash
journalctl -u bot@xx -n 100
```

## Status/start/stop

```bash
systemctl status bot@01.service
systemctl stop bot@01.service
systemctl start bot@01.service
```

## Updating maps

Done by hand from your local system.
Requires access to vault secret


```bash
# setup vault_password file
cd triplea/infrastructure
echo "<vault secret>" > vault_password
```

Run update script:
```bash
cd triplea/infrastructure
./run_ansible_update_bots
```

