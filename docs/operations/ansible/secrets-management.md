- [Secret Management](#secret-management)
  - [Vault Password](#vault-password)
  - [Creating Secrets](#creating-secrets)
  - [Encrypting variables](#encrypting-variables)
  - [Ansible Vault File Encryption](#ansible-vault-file-encryption)


# Secret Management

Documentation for how passwords can be securely checked in and managed.

## Vault Password

To run ansible, you will need to create a file named 'vault_password'
and add to that file the ansible vault passowrd (project admins/maintainers
will have this).

```
cd infrastructure/
touch vault_password
# edit 'vault_password' and add the ansible vault password
```

## Creating Secrets

## Encrypting variables

Encrypted variables can be placed in a `defaults/main.yml` file and
will be decrypted by ansible when ansible is run. To encrypt a variable:

1. Create a file named: 'vault_password' and place the vault_password
   in that file
1. Create a file named: 'secret' and place the secret value to be
   encrypted in that file

```
./create_secret "name_of_variable"
```

[Ansible-Vault Docs](https://docs.ansible.com/ansible/latest/user_guide/vault.html)

Warnings:

* use files to store passwords/secrets so that the password is not in your
  shell history
* take care to not commit into git any passwords or secrets, files containing
  secrets should be added to .gitignore to help prevent this.
* if any secret is exposed, we would need to rotate password and
  re-encrypt variables

## Ansible Vault File Encryption

For reference, encrypting a file looks like this:

```
ansible-vault encrypt --vault-password-file=vault_password ansible_ssh_key.ed25519
```

