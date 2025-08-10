# Server Ops

## Access

Done via SSH key, key is added in ansible
config and is deployed to each server.

```bash
ssh admin@<lobby-dns-address>
```

### Status & Restart

```bash
sudo systemctl status lobby_server
sudo systemctl stop lobby_server
sudo systemctl start lobby_server
```

```bash
sudo ps -ef | grep java

# Expected output'ish:
lobby_s+   55674       1 99 05:16 ?        00:00:01 /usr/bin/java -jar bin/triplea-lobby-server-2.6.jar
```

## Logs

```bash
journalctl -u http_server

# last 100 lines
journalctl -u lobby_server -n 100
journalctl -u lobby_server -f
```

### Connect to database & view tables

```bash
sudo -u postgres psql
\c lobby_db
\d

```

Restart database:

```
sudo service postgresql reload
```

### Check Nginx

```bash
sudo systemctl status nginx
```

