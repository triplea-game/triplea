## Troubleshooting

If nginx fails to reload or to start up:

```bash
cd infrastructure/vagrant
vagrant ssh
sudo /usr/sbin/nginx
```

The above will usually print out a more detailed error message
that should tell you where the problem is.

After you fix the configuration problem, reload nginx:
```
cd infrastructure/vagrant
vagrant ssh
sudo systemctl reload nginx
```
