# Local Development with Vagrant

Ansible configuration development can be done locally with vagrant.
Vagrant provides a virtual machine that can be used as the target for
deployments. This avoids the need for a live linode server to verify
updates and/or build new roles.

## Installation

### (1.A) Install, VirtualBox and Ansible

```bash
sudo apt install -y virtualbox ansible
``

### (1.B) Install Vagrant

Install from the download site: https://www.vagrantup.com/downloads.html

The version in 'apt' is likely to be out of date and could have Ruby errors.

Once downloaded, unzip somewhere, add that somewhere to your path so the
command "vagrant" is available. If not added to your path, fully quality
the command 'vagrant' in the steps below to match.


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

### HTTPS Config
Ansible will setup a self-signed certificate to be used by nginx. So in order to actually access the server without any exception thrown by the TLS layer, you'll have to import this certificate into your OS' root certificates.
In order to extract the certificate on to your system, you'll have to install the vagrant-scp plugin like this:
```bash
vagrant plugin install vagrant-scp
```
Then you can copy it to a convenient place on your system in order to import it.
On Ubuntu you'd run:
```bash
sudo vagrant scp :/etc/nginx/cert.crt /usr/local/share/ca-certificates/triplea_vagrant.crt
sudo chmod 644 /usr/local/share/ca-certificates/triplea_vagrant.crt
sudo update-ca-certificates
```
to import the certificate into your local keystore.
Now you can use the self-signed certificate like any other CA-validated certificate.
You'll have to do this every time the self-signed certificate expires or is replaced manually.
Currently it's valid for 356 days.

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
