# Server Ops


## Logs
Page through all logs
```
journalctl -r
```

### http_server logs

```bash
journalctl -u http_server
```

last 100 lines
```bash
journalctl -u http_server -n 100
```

## Service Ops

### Bot
```bash
systemctl status bot@01.service
systemctl stop bot@01.service
systemctl start bot@01.service
```

### http_server

```bash
systemctl status http_server.service
systemctl stop http_server.service
systemctl start http_server.service
```

