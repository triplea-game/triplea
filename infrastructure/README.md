
## Pre-Requirements

- `ansible-playbook`, can get this by:
  - installing ansible
  - or docker images available


## Typical Commands

Vagrant commands are run from the same folder containing a `VagrantFile` 
and docker commands from a `docker-compose.yml` file.

```bash
cd triplea/infrastructure
```


### Run Everything

```bash
./build_all
```

### Launch Vagrant Servers
```bash
vagrant up
```

### Check Server Status

```bash
vagrant status
docker container ls
```

### Connect to vagrant server
```
vagrant ssh (serverName)
```

*serverName* = (lobby|http_server|bot)


#### Viewing Logs

When connected to vagrant server:
```bash
sudo journalctl -u http-server
```

This can be used to ssh to the VM, eg:
```bash
ssh -i .vagrant/machines/lobby/virtualbox/private_key vagrant@127.0.0.1 -p 2020
```

The `VagrantFile` has the port forwarding numbers for each VM.


### Run Ansible

```bash
run_ansible
```


### Clean / Destroy Virtual Servers

```bash
vagrant destroy -f
```

`vagrant halt` to simply stop the servers


# TODO


## [ ] docker-compose.yml

```yaml
version: '3'
services:
  dns:
    container_name: dns-proxy-server
    image: defreitas/dns-proxy-server
    ports:
      - 5380:5380
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock 
      - /etc/resolv.conf:/etc/resolv.conf 
  postgres:
    container_name: database
    image: postgres:9.5
    hostname: db
    network_mode: bridge
  lobby:
    container_name: lobby
    # build: docker/lobby/
    image: alpine:3.9
    hostname: lobby
    network_mode: bridge
    volumes:
      - ./keys:/root/.ssh
    CMD: tail -f /dev/null 
  ansible:
    #  Example: https://github.com/philm/ansible_playbook/blob/master/test/docker-compose.yml
    image: philm/ansible_playbook
    #image: solita/ansible-ssh
    network_mode: bridge
    volumes:
      - ./ansible:/ansible/playbooks
```

## [ ] docker-compose runs on branch builds

- travis can launch dockers, launch a DB and lobby.
- add and executes integration level tests to verify lobby.

## [ ] ansibleMasterRole

- should check for updated versions on github releases
  - execute staging and production ansible deployments on new releases.


## [ ] FlyWay Migrations

- On staging and production we should use flyway to do DB migrations.


## [ ] integrate with pre-release testing

- see if we can fail the build on pre-release, post-merge if testing fails
- run ansible at the pre-release phase before travis deploys to github release to see if alive
server can take a deployment and then pass if software connectivity checks look good.


## [ ] certbot from letsencrypt


Draft so far of what this needs, need to get this fully working and then converted to an ansible role:

```bash
sudo apt-get update
sudo apt-get install software-properties-common
sudo add-apt-repository universe
sudo add-apt-repository ppa:certbot/certbot
sudo apt-get update
sudo apt-get install certbot python-certbot-nginx 

sudo certbot --test-cert --nginx -m tripleabuilderbot@gmail.com --agree-tos
```

## [ ] Proper SSL certificate for http-server

- right now we generate a self-signed cert and client has certificate validation disabled

