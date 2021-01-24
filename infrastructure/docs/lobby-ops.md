# Server Ops

## Access

Done via SSH key, key is added in ansible
config and is deployed to each server.

```bash
ssh admin@<lobby-dns-address>
```

## Stop/Restart/Start

Done via systemctl, there are start/stop
scripts deployed to /home/admin.

## Logs

```bash
journalctl -u http_server

# last 100 lines
journalctl -u lobby_server -n 100
```

## Log into database

SSH to lobby server

```bash
sudo -u postgres psql
```

