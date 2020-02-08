# Ansible Overview

Documentation: https://github.com/triplea-game/triplea/wiki/Ansible-Documentation


## Running Ansible

- Prerelease is automatic
  - Deployments are run as the last step of travis builds, after artifacts
are uploaded to github releases. A utility script will download those artifacts
and place them in a location ansible can find them, then deployment to
prerelease will start which updates/upgrades/installs to the servers
defined in the prerelease inventory file.

- To run full stack locally, see the vagrant readme file in this same folder.
- To run deployment manually:

Assuming a vault password file named 'vault_password' is created in the same
folder and contains the ansible vault password, run: 

```bash
./run_deployment [version_to_install] [ansible_args]
```

examples


### Typical Deployment Command

```bash
./run_deployment 2.0.1000 -i ansible/inventory/prerelease
```

### Useful flags

* `-v`: Verbose output
* `-vvv`: Debug output
* `-vvvv`: SSL Debug output
* `--diff`: Shows differences in updated templates and files
* `-t`: Tags

### Examples with Tags


#### Deploy just bots

```bash
./run_deployment 2.0.1000 -t bots -i ansible/inventory/prerelease
```

#### Deploy NGINX and CertBot

```bash
./run_deployment 2.0.1000 -t nginx,certbot -i ansible/inventory/prerelease
```

## Example Prod Deployment

Production deployment is only a matter of specifying the production inventory file.

```bash
./run_deployment 2.0.1000 -i ansible/inventory/production
```

## PreRelease and Production Deployments

- Executed as part of travis pipeline build after release artifacts are generated
and deployed to github releases.
- Variable values, such as passwords are kept constant between prerelease and production
- Production version is controlled by a variable, prerelease is always latest version
- Prerelease specific deployment instructions are excluded via ansible 'if' instructions,
  when promoting such steps to production, we remove those if statements.
- Production deployment occurs on every build, ansible is idempotent by design,
  this allows us to ensure updates, update/add/change servers from inventory files


## Vault Password

To run ansible, you will need to create a file named 'vault_password' 
and add to that file the ansible vault passowrd (project admins/maintainers will have this).

```
cd infrastructure/
touch vault_password
# edit 'vault_password' and add the ansible vault password
```

# Creating Secrets

### Encrypting variables

Encrypted variables can be placed in a `defaults/main.yml` file and will be decrypted
by ansible when ansible is run. To encrypt a variable:

1. Create a file named: 'vault_password' and place the vault_password in that file
1. Create a file named: 'secret' and place the secret value to be encrypted in that file
```
./create_secret "name_of_variable"
```

[Ansible-Vault Docs](https://docs.ansible.com/ansible/latest/user_guide/vault.html)

Warnings:

* use files to store passwords/secrets so that the password is not in your
  shell history
* take care to not commit into git any passwords or secrets, files containing
  secrets should be added to .gitignore to help prevent this.
* if any secret is exposed, we would need to rotate password and re-encrypt variables


### Ansible Vault File Encryption

For reference, encrypting a file looks like this:
```
ansible-vault encrypt --vault-password-file=vault_password ansible_ssh_key.ed25519
```

