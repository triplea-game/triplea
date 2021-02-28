# Lobby Ops

### Check Logs
```bash
sudo journalctl -u lobby_server -n 300
sudo journalctl -u lobby_server -f
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

### Connect to database & view tables

```bash
sudo -u postgres psql
\c lobby_db
\d
```

### Check Nginx

```bash
sudo systemctl status nginx
```
