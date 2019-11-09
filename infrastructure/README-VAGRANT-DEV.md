# Local Development with Vagrant

Ansible configuration development can be done locally with vagrant.
Vagrant provides a virtual machine that can be used as the target for
deployments. This avoids the need for a live linode server to verify
updates and/or build new roles.

## Installation

### (1) Install Vagrant, VirtualBox and Ansible

```bash
sudo apt install -y virtualbox vagrant ansible
```

### (2) Launch Vagrant virtual machine
```bash
cd ~/triplea/infrastructure
vagrant up
```

*Note*, you may need to do some chowning to be able to run virtualbox as non-root:
```bash
sudo chown $USER:$USER -R ~/.vagrant.d/
sudo chown $USER:$USER -R ~/triplea/infrastructure/.vagrant/
```

## Run Ansible

```bash
./run_ansible_vagrant
```

## Check Results

```bash
cd ~/triplea/infrastructure
vagrant ssh

## check apps are running
ps -ef | grep java

## check logs
journalctl -f -u triplea-lobby

## log in to database and verify DB and tables exist
sudo -u postgres psql
```

## Clean / Destroy Virtual Servers

```bash
vagrant destroy -f
```


# TODO

## [ ] Execute 'run_deployment' for prerelease from travis after successful merge
- will need to install sshpass on the travis machine
- will need to set ansible password on travis as env variable
- will need to add sshpass+ssh command to execute 'run_deployment'

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
