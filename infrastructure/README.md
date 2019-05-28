
# Executing a Deployment

- Use linode console and SSH to the infrastructure machine as root
- Install your public ssh key in: `/home/ansible/.ssh/authorized_keys`
- SSH to the infrastructure machine as user ansible
- Run: `./run_deployment <VERSION> [prerelease|production]`


# Local Development

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
./run_ansible
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


# Testing on PreRelease Server


Manually copy a ansible.zip file to the infrastructure machine, comment out the run_deployment
steps that download ansible.zip and then execute 'run_deployment' from the infrastructure
machine.


```bash
cd ~/triplea/infrastructure/ansible
rm -f ansible.zip
zip -r ansible.zip *
scp ansible.zip ansible@173.255.247.175:~/
```

```bash
ssh ansible@173.255.247.175
rm -rf ansible/ bot/ lobby/
unzip ansible.zip -d ansible/
## update "run_deployment" and comment out the download function for ansible.zip
./run_deployment <version> prerelease
```


## Infrastructure Server

### How the infrastructure server is used

The infrastructure server is where we run ansible. The general process
is that we do a one-time setup to configure access and install ansible.
From there we have a 'run_deployment' script on the infrastructure server
we would use to execute deployments. The difference between prerelease and
production deployments will be just which inventory file is used.


### Setup Notes

These were one-time setup to configure the infrastructure machine:

- Update server host name in `/etc/hostname` to `infrastructure.triplea-game.org`

- Reboot

- Add ansible user

```bash
adduser -m ansible
```

- Allow password login for ansible, update `/etc/ssh/ssh` to have following:
```bash
ChallengeResponseAuthentication yes
PasswordAuthentication yes
PermitRootLogin no
```

Then reload sshd for settings to take effect:
```bash
service sshd reload
```

(note: to get root access, login as ansible first, then `su` to root)


- As root:

```bash
ufw enable
ufw allow 22
apt install -y ansible \
    fail2ban \
    unattended-upgrades \
    shellcheck \
    unzip
```

Copy `secrets` and `run_deployment` from `~/triplea/infrastructure/infrastructure-machine`
to the infrastructure machine. Update the values in secrets to be real secret values.

Switch to ansible user, generate a ssh key (use elliptic curve algorithm).

### Access to infrastructure server

Login from linode console using user: `root` and password.

Add your ssh public key to `/root/.ssh/authorized_keys`


## Setting up target machines

This will need to be done once for any new linode server, largely
we are just creating an 'ansible' user that ansible will use
to remote execute deployment commands:

```bash
sudo useradd -m ansible
sudo mkdir /home/ansible/.ssh
sudo chown ansible:ansible /home/ansible/.ssh
sudo chmod 700 /home/ansible/.ssh
echo "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIJWlr72ICC96nQjLLPN5kcEJ6yCjKv8SMoXQEBWyZRy1 ansible@infrastructure.triplea-game.org" | sudo tee /home/ansible/.ssh/authorized_keys
sudo chown ansible:ansible /home/ansible/.ssh/authorized_keys
sudo usermod -p '*' ansible
```

Note, the ssh key above is already generated on the infrastructure machine.


Add ansible to sudoer, add the following line to `/etc/sudoer`:
```bash
ansible ALL=(ALL) NOPASSWD: ALL
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
