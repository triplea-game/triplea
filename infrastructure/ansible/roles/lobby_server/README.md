# Lobby_Server role

- runs DB schema migrations
- deploys nginx config file for reverse proxy
- deploys and configures lobby application, installed
  as 'lobby_server_<version>', eg: 
    `sudo systemctl status lobby_server_2.6`

## SSL Setup

We use 'certbot' to create 'LetsEncrypt' signed certificates.
It is not easy to automate. We also have another challenge
that getting a cert requires a DNS entry, which is not
available  on a local installation to vagrant.

When deploying to vagrant, we will generate self signed certificates.

After running the deployment, run certbot manually with the
following commands:

```bash
apt install \
  software-properties-common \
  certbot \
  python3-certbot-nginx

certbot \
 --nginx \
 -d <domain_name> \
 --rsa-key-size 4096
```

Once certbot is run, run a deployment in 'check' and 'diff'
mode to validate which changes would be made on the next
deployment. Check in any updates such that this 'diff'
is empty.
