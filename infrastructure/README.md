# Infrastructure Project

Hosts [ansible](https://www.ansible.com) code that maintains
[infrastructure as code](https://en.wikipedia.org/wiki/Infrastructure_as_code)

In short, we should never log in to a server, all changes are checked
in and then applied by running a deployment. This creates
a consistent and documented server state that anyone can reproduce.

More operations details are on the [wiki](https://github.com/triplea-game/triplea/wik)

## Deployments

Deployments to prerelease and production are automated.
Prerelease will receive the latest code after every merge. Production
deployments are controlled by a version number variable in configuration.
If the version value does not change, the deployment is idempotent
and nothing will change on the server.

Deployments are triggered as part of
[travis](https://travis-ci.org/github/triplea-game/triplea) builds.

## Production Deployment

It can be useful to make changes locally, run a production deployment,
and then check in the changes after-the fact. This is very risky,
user beware. Typical command to do this will be with a limit
to restrict deployments to specific hosts. EG:

```
./run_ansible_production --limit prod2-bot02.triplea-game.org
```

## Ansible Structure

Ansible has three parts:

- inventory (lists hosts)
- roles (can be thought of as applications)
- playbooks, binds hosts to roles.

The only different between environments should be the list
of hosts in an inventory file. Each environment (IE: prerelease, prod)
will have its own inventory file. Each inventory file has the same
set of hostgroups, only the hostnames are different between them.

Each [role](./ansible/roles) should have a short README.md file that
describes  what is deployed by that role.

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
