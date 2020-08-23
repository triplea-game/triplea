- [Ansible Overview](#ansible-overview)
  - [Variables](#variables)
    - [Role Defaults](#role-defaults)
    - [Group vars (all.yml - shared variables)](#group-vars-allyml---shared-variables)
    - [Group vars (environment/hostgroup specific)](#group-vars-environmenthostgroup-specific)
    - [Notes on variables](#notes-on-variables)
  - [Ansible Public Key](#ansible-public-key)
- [Https Certificate Installation](#https-certificate-installation)

# Ansible Overview

Deployment is done with [ansible](https://www.ansible.com/)

* Deployments are idempotent: https://shadow-soft.com/ansible-idempotency-configuration-
drift/

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




# Https Certificate Installation

The 'certbot' role will:

* run lets-encrypt and create a publicly signed SSL cert.
* sets up a weekly renewal cronjob that will renew the SSL cert if it
    is within 30 days of expiry

For each domain running SSH, a CAA DNS record needs to be created (one time):

![Screenshot from 2019-11-19 13-06-13](https://user-images.githubusercontent.com/12397753/69196411-48980e00-0ae3-11ea-9130-61e1fd5368b3.png)

