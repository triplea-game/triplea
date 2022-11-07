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

```bash
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
localhost$ vagrant status

## vagrant ssh will connect you to the running virtualbox instance
localhost$ vagrant ssh

## check apps are running
vagrant$ ps -ef | grep java

## check lobby logs
vagrant$ journalctl -f -u triplea-lobby

## log in to database and verify DB and tables exist
vagrant$ sudo -u postgres psql
```

```bash
# Halt the Virtual Server
cd ~/triplea/infrastructure/vagrant
vagrant halt

# Clean / Destroy Virtual Servers
cd ~/triplea/infrastructure/vagrant
vagrant destroy -f
```

## Troubleshooting

### VM Won't Start

- Open VirtualBox, the VM should be listed there and you can open a console to 
  the VM from VirtualBox which will show you the latest console output from
  the VM. This console output may give an indication of what is happening
- Sometimes bringing the VM down and recreating it is the way to go:

```bash
cd triplea/infrastructure/vagrant/
vagrant halt
vagrant destroy
vagrant up
```
