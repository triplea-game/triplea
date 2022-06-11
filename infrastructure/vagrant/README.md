## Local Development with Vagrant

Vagrant allows you to launch virtual machines via CLI. This can be used
to simulate deployments to a production server.

Vagrant typically interacts with VM software like VirtualBox which is
the software that is actually launching the virtual machine.

## Quick Start

### Installation

```bash
sudo apt install -y virtualbox ansible
```

Install Vagrant from [vagrant download site](https://www.vagrantup.com/downloads.html)


### Start VM & Run Deployment

```bash
cd .../triplea/infrastructure/vagrant
vagrant up
cd ..
./run_ansible --env vagrant
```

Once the above is completed, vagrant will have launched a VM and ansible
will have deployed a full lobby server to the machine, available at:
`https://localhost:8000`

To access the VM:
```
cd .../triplea/infrastructure/vagrant
vagrant ssh
```

*Note*, you may need to do some chowning to be able to run virtualbox as non-root:

```bash
sudo chown $USER:$USER -R ~/.vagrant.d/
sudo chown $USER:$USER -R ~/triplea/infrastructure/.vagrant/
```

### More Useful Commands


```bash
cd ~/triplea/infrastructure/vagrant

## check status of vagrant VMs:
vagrant status

## vagrant ssh will connect you to the running virtualbox instance
vagrant ssh

## check apps are running
ps -ef | grep java

## check lobby logs
journalctl -f -u triplea-lobby

## log in to database and verify DB and tables exist
sudo -u postgres psql
```

### Clean / Destroy Virtual Servers

```bash
vagrant destroy -f
```
