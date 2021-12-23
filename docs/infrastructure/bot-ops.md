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

Done via github actions:
  github.com > actions > 'run bot update maps' > run-action

