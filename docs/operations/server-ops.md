# Server Ops


## Logs
Page through all logs
```
journalctl -r
```

### lobby_server logs

```bash
journalctl -u http_server
```

last 100 lines
```bash
journalctl -u lobby_server -n 100
```

## Service Ops

### Bot
```bash
systemctl status bot@01.service
systemctl stop bot@01.service
systemctl start bot@01.service
```

### lobby_server

SSH to the lobby server, note that there are start/stop scripts deployed
to /home/admin.
