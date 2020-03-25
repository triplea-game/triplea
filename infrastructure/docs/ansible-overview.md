# Ansible Overview

Deployment is done with [ansible](https://www.ansible.com/)

* Deployments are [idempotent](https://shadow-soft.com/ansible-idempotency-configuration-
drift/)

Ansible consists of three main components:
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

