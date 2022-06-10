## Local Development with Vagrant

Vagrant allows you to launch virtual machines via CLI. This allows for
automation of creating virtual machines where deployments can be tested.

VAgrant typically interacts with VM software like VirtualBox which is
the software that is actually launching the virtual machine.

### Installation

#### (1.A) Install, VirtualBox and Ansible

```bash
sudo apt install -y virtualbox ansible
```

#### (1.B) Install Vagrant

Install from [vagrant download site](https://www.vagrantup.com/downloads.html)

The version in the standard 'apt' repositories is likely to be out of date
and could have Ruby errors.

#### (2) Launch Vagrant virtual machine

```bash
cd ~/triplea/infrastructure/vagrant
vagrant up
```

*Note*, you may need to do some chowning to be able to run virtualbox as non-root:

```bash
sudo chown $USER:$USER -R ~/.vagrant.d/
sudo chown $USER:$USER -R ~/triplea/infrastructure/.vagrant/
```

### Run Ansible

```bash
cd ~/triplea/infrastructure/
./run_ansible --environment vagrant
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
