# Infrastructure Project

This project hosts the code and configuration that controls
deployments. For design, conventions and a description of 
the deployment process, please see the documentation in
 the [/docs](/docs) folder.

## Local Development with Vagrant

- Deployments can be tested using Vagrant. Vagrant allows you to launch
virtual machines via CLI.

- Use vagrant to test deployments and configuration updates.

- Don't worry, the initial setup is not too bad and once done it's pretty easy.

### Installation

#### (1.A) Install, VirtualBox and Ansible

```bash
sudo apt install -y virtualbox ansible
```

#### (1.B) Install Vagrant

Install from the [vagrant download site](https://www.vagrantup.com/downloads.html)

The version in 'apt' is likely to be out of date and could have Ruby errors.

Once downloaded, unzip somewhere, and add to your path so you have
the command 'vagrant ' available.

#### (2) Launch Vagrant virtual machine

```bash
cd ~/triplea/infrastructure
vagrant up
```

*Note*, you may need to do some chowning to be able to run virtualbox as non-root:

```bash
sudo chown $USER:$USER -R ~/.vagrant.d/
sudo chown $USER:$USER -R ~/triplea/infrastructure/.vagrant/
```

### Run Ansible

```bash
./run_ansible_vagrant
```

#### HTTPS Config

Ansible will setup a self-signed certificate to be used by nginx.
`run_ansible_vagrant` will symlink this certificate from the vagrant virtual
machine to your local '/usr/local/share/ca-certificates' where it will
be picked up and added as a trusted certificate.

### Check Results

```bash
cd ~/triplea/infrastructure
## vagrant ssh will connect you to the running virtualbox instance
vagrant ssh

## check apps are running
ps -ef | grep java

## check logs
journalctl -f -u triplea-lobby

## log in to database and verify DB and tables exist
sudo -u postgres psql
```

### Clean / Destroy Virtual Servers

```bash
vagrant destroy -f
```
