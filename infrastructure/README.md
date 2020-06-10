# Infrastructure Project

Home to infrastructure deployment, notably done with a technology
called [ansible](https://www.ansible.com)

See the "operations" section of
[wiki](https://github.com/triplea-game/triplea/wik)
for more documentation, on how to add servers, run ad-hoc commands,
check logs etc..

Infrastructure deployment refers to how we set up and deploy software
to test and production servers.

Notably ansible is designed to be idempotent, we run deployments to
prerelease and production on every merge to master. This is triggered
from [travis](https://travis-ci.org/github/triplea-game/triplea) which
builds the TripleA master branch after each PR is merged.

Each ansible 'role' can be thought of as an application that is deployed.
Roles should be pretty atomic and granular so that we can easily configure
and re-use them between different hosts. Each [role folder](./ansible/roles)
should have a README.md file that describes what is deployed by that role.

## Local Development with Vagrant

Ansible configuration development can be done locally with vagrant.
Vagrant provides a virtual machine that can be used as the target for
deployments. This avoids the need for a live linode server to verify
updates and/or build new roles.

### Installation

#### (1.A) Install, VirtualBox and Ansible

```bash
sudo apt install -y virtualbox ansible
```

#### (1.B) Install Vagrant

Install from the [vagrant download site](https://www.vagrantup.com/downloads.html)

The version in 'apt' is likely to be out of date and could have Ruby errors.

Once downloaded, unzip somewhere, add that somewhere to your path so the
command "vagrant" is available. If not added to your path, fully quality
the command 'vagrant' in the steps below to match.

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
