## Ansible Public Key

When creating a new linode, add this public key to your account and then select for it to be added
to the new linode. This key is used by ansible to gain server access.

> ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIBdU9dU02UR5MCutULVgpdT1mN6wjJOKL8sW1/ZZkdym ansible-public-key

### Running

Will need to create a 'vault_password' file containing the ansible vault passowrd:

```
cd infrastructure/
# create file 'vault_password' containing the ansible vault password
```

Encrypting a file looks like this:
```
ansible-vault encrypt --vault-password-file=vault_password ansible_ssh_key.ed25519
```

### Creating Secrets

* Create a file "vault_password" and place vault_password in the file
* Create a file 'secret' and place the secret to encrypt in the file
```
ansible-vault encrypt_string --vault-password-file vault_password "$(cat secret)" --name 'the_secret'
```


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

## Ansible-vault / Secrets

[Ansible-Vault Docs](https://docs.ansible.com/ansible/latest/user_guide/vault.html)



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
