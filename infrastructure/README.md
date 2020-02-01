# Ansible Overview

Deployment is done with [ansible](https://www.ansible.com/)

* Deployments are idempotent: https://shadow-soft.com/ansible-idempotency-configuration-drift/

* Ansible consists of three main components:
  * roles: think of these as applications
  * inventory files: lists servers or hosts by group
  * playbooks: binds host groups to roles

* Per environment configuration is done through hostgroups, defined in inventory
files. For example the production inventory file will have a hostgroup called
'production' and we we can then define a 'group_vars/production.yml' file that
has variable values that only apply to the 'production' hosts. These variable
values will 'override' any variable values defined in 'defaults'.

## Variables

* Ansible allows variables to be defined in many places with differing
levels of precendence. For simplicity,  in this project we constrain where
variables are defined.

### Role Defaults

These variables will be in 'roles/[role_name]/defaults/main.yml'.
Roles should define a default value for all variables used exclusively
by that role.

### Group vars (all.yml - shared variables)

'groups_vars/all.yml' is a file that applies to all host groups.
Variables that are shared between multiple roles are defined here.

### Group vars (environment/hostgroup specific)

For variables that are both shared and environment specific, we will
define them in a group_vars file. For example: "database_password"
may be defined in "groups_vars/production.yml" to have one value
on production, and also defined in "group_vars/vagrant.yml" to
have another value when deploying to a local vagrant server.

The inventory files should all have the same structure and
define hostgroups that line up with the "group_var" file names.


### Notes on variables

* do not use `vars` folder
* variable names should be lower_snake_case
* do not use 'dashes' in variable names. Dashes are used to denote default values.
  A variable named "my-value" will likely result in a "variable 'my' not found"
* favor placing variables in defaults unless they are shared or vary by environment

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

## Ansible Public Key

Ansible needs to communicate to target servers via ssh. Locally we have a private key
that is encrypted and decrypted when ansible runs (decryption is via ansible vault).
To enable this, we need the ansible public key to be deployed to the target server under
the root users 'authorized_keys' file.

The installation of a public key to root user can be done during linode creation from the
lindoe web UI. Add this public key to your linode account profile (via the linode website):

> ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIBdU9dU02UR5MCutULVgpdT1mN6wjJOKL8sW1/ZZkdym ansible-public-key

Then, when creating a new linode, select that public key and it will added to the root user
'authorized_keys' file.


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
 - use files to store passwords/secrets so that the password is not in your shell history
 - take care to not commit into git any passwords or secrets, files containing secrets should
   be added to .gitignore to help prevent this.
 - if any secret is exposed, we would need to rotate password and re-encrypt variables


### Ansible Vault File Encryption

For reference, encrypting a file looks like this:
```
ansible-vault encrypt --vault-password-file=vault_password ansible_ssh_key.ed25519
```

# Https Certificate Installation

The 'certbot' role will:
  - run lets-encrypt and create a publicly signed SSL cert.
  - sets up a weekly renewal cronjob that will renew the SSL cert if it
    is within 30 days of expiry

For each domain running SSH, a CAA DNS record needs to be created (one time):

![Screenshot from 2019-11-19 13-06-13](https://user-images.githubusercontent.com/12397753/69196411-48980e00-0ae3-11ea-9130-61e1fd5368b3.png)


