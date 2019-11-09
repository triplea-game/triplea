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


# Running Ansible

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

## Executing a Deployment

Assuming a vault password file is created, run: `./run_deployment <VERSION> [prerelease|production]`


## PreRelease and Production Deployments

- Executed as part of travis pipeline build after release artifacts are generated
and deployed to github releases.
- Variable values, such as passwords are kept constant between prerelease and production
- Production version is controlled by a variable, prerelease is always latest version
- Prerelease specific deployment instructions are excluded via ansible 'if' instructions,
  when promoting such steps to production, we remove those if statements.
- Production deployment occurs on every build, ansible is idempotent by design,
  this allows us to ensure updates, update/add/change servers from inventory files


# Creating Secrets

### Encrypting variables

Encrypted variables can be placed in a `defaults/main.yml` file and will be decrypted
by ansible when ansible is run. To encrypt a variable:

1. Create a file named: 'vault_password' and place the vault_password in that file
1. Create a file named: 'secret' and place the secret value to be encrypted in that file
```
VARIABLE_NAME="name_of_the_secret_variable"
ansible-vault encrypt_string --vault-password-file vault_password "$(cat secret)" --name "$VARIABLE_NAME"
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
