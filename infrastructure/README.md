# Ansible Overview

Deployment is done with [ansible](https://www.ansible.com/)
In short, ansible will run commands to assert state on a target machine.
Asserting state means, for example, that a certain system user should exist,
if not, ansible will create it. In this ansible is idempotent by design.

Ansible is driven by the concept of a playbook. It ties an inventory
file, which contains a set of hosts defined by groups, to 'roles'. Roles
can be thought of as an applications. The roles are defined by the
folders in the 'roles' folder. Ansible roles have a standard file format
where variables are defined. In this project the two files of most
note are "defaults/main.yml", which defines all variables to be used
in a role, and "tasks/main.yml" which defines a series of tasks to execute.

It's also notewory that 'group_vars' can be used to define environment
specific variables. Ansible has a precedence ordering for variables,
'group_vars' have high precedence, they override most other values,
'defaults' have lowest.
So you may see for example a default variable that is defined like 
`bot_lobby_uri: "{{ lobby_uri }}"`
That essentially means there is no default value for the variable
and we expect the variable to be defined in group_vars.

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

``bash
./run_deployment [version_to_install] [ansible_args]
```

examples
```bash
./run_deployment 2.0.1000 -i ansible/inventory/prerelease

# To see verbose output
./run_deployment 2.0.1000 -v -i ansible/inventory/prerelease

# To see 'diff' output, which shows how each file on server is being updated
#  (caution!! this can expose secret values to stdout)
./run_deployment 2.0.1000 -d -i ansible/inventory/prerelease

# To see SSL debug output:
./run_deployment 2.0.1000 -vvv -i ansible/inventory/prerelease

# To deploy to just bots, use tags, '-t' (see playbook for tag names)
./run_deployment 2.0.1000 -vvv -t bots -i ansible/inventory/prerelease
```

Production deployment is only a matter of specifying the production inventory file.

```
./run_deployment 2.0.1000 -vvv -i ansible/inventory/production
```

## Vault Password

To run ansible, you will need to create a file named 'vault_password' 
and add to that file the ansible vault passowrd (project admins/maintainers will have this).

```
cd infrastructure/
touch vault_password
# edit 'vault_password' and add the ansible vault password
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

# Ansible Variables

## Shared Variables

Variables that are needed across multiple roles will be defined in `group_vars/all.yml`
Roles should otherwise try to be as self contained as possible.

## Naming & Conventions

Variables will be prefixed by the role where they are used. For example a role named "foo" will
variables like `foo_home`, `foo_version`. In role `tasks/main.yml`, we should try to refer only
to variables defined in the defaults for that role, ie: `defaults/main.yml`

### Example
If we have a shared database password, in `group_vars/all.yml` we might have (in a real example
this would be encrypted):
```yaml
db_password: 123
```

In role foo, referencing this shared value, `defaults/main.yml` would assign this value with:
```yaml
foo_db_password: "{{ db_password }}"
```
Then in any tasks or templates for role 'foo', we would use the role specific variable,
ie: `{{ foo_db_password }}`

## Variable Locations
- do not use `vars` folder
- define shared variables and environment specific overrides in `group_vars`
- all other variables should be defined in `defaults/main.yml`

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

Currently done manually.

## certbot from letsencrypt

```bash
sudo apt-get update
sudo apt-get install software-properties-common
sudo add-apt-repository universe
sudo add-apt-repository ppa:certbot/certbot
sudo apt-get update
sudo apt-get install certbot python-certbot-nginx 

sudo certbot --nginx -m tripleabuilderbot@gmail.com --agree-tos
```

Create CAA DNS records

![Screenshot from 2019-11-19 13-06-13](https://user-images.githubusercontent.com/12397753/69196411-48980e00-0ae3-11ea-9130-61e1fd5368b3.png)

Everything that goes well, should look like:
```
Congratulations! You have successfully enabled
https://prerelease.triplea-game.org

You should test your configuration at:
https://www.ssllabs.com/ssltest/analyze.html?d=prerelease.triplea-game.org
- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

IMPORTANT NOTES:
 - Congratulations! Your certificate and chain have been saved at:
   /etc/letsencrypt/live/prerelease.triplea-game.org/fullchain.pem
   Your key file has been saved at:
   /etc/letsencrypt/live/prerelease.triplea-game.org/privkey.pem
   Your cert will expire on 2020-02-17. To obtain a new or tweaked
   version of this certificate in the future, simply run certbot again
   with the "certonly" option. To non-interactively renew *all* of
   your certificates, run "certbot renew"
 - Your account credentials have been saved in your Certbot
   configuration directory at /etc/letsencrypt. You should make a
   secure backup of this folder now. This configuration directory will
   also contain certificates and private keys obtained by Certbot so
   making regular backups of this folder is ideal.
``````

