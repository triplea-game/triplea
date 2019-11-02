
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


# PreRelease and Production Deployments

- Executed as part of travis pipeline build after release artifacts are generated
and deployed to github releases.
- Variable values, such as passwords are kept constant between prerelease and production
- Production version is controlled by a variable, prerelease is always latest version
- Prerelease specific deployment instructions are excluded via ansible 'if' instructions,
  when promoting such steps to production, we remove those if statements.
- Production deployment occurs on every build, ansible is idempotent by design,
  this allows us to ensure updates, update/add/change servers from inventory files
- Variables are stored in travis online website configuration

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
