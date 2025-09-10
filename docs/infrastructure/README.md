# Infrastructure Project

[/infrastructure](/infrastructure) Hosts the code and configuration that
controls deployments. To modify what is run on the servers, we modify
the checked-in configuration. After merging in configuration changes,
automation will pick up and deploy these changes to the production servers.

## Quickstart

```bash
cd infrastructure/vagrant
vagrant up
cd ../../
./infrastructure/run_ansible --env vagrant
```
Lobby will be deployed to an Ubuntu VM launched by Vagrant running on VirtualBox.
To connect, open game settings, testing, and connect to lobby address
'https://localhost:8000'. Be sure to install cert to your OS.

### Install Lobby SSH Cert on Your Local OS

Add the checked-in file: 'vagrant-only.cert.crt', to your operating system's trusted certs.

On Linux/Ubuntu:
```bash
sudo cp triplea/infrastructure/ansible/roles/nginx/files/vagrant-only.cert.crt \
  /usr/local/share/ca-certificates/
sudo update-ca-certificates
```

## Working With Ansible

You can use 'ansible' to run deployments against a local virtual machine.
Ansible files are in: [/infrastructure/ansible](/infrastructure/ansible)

Once you have a local virtual machine launched using 'vagrant', you can
run ansible deployments with a simple wrapper script: 'run_ansible'

For how to set up and work with vagrant, see:
[/infrastructure/vagrant/README.md](/infrastructure/vagrant/README.md)

### --diff flag

You can toggle a '--diff' flag to see what changes ansible is doing
while it does them. If you run ansible a second time, you will see that
there are no changes made (server is in fully configured state).

### --dry-run flag
You can add a '--dry-run' flag to enable ansible to run in a 'check mode'.
When in 'check mode', ansible will only simulate changes and will not actually
make any updates.

### Use 'diff' and 'dry-run' to preview changes before committing them

Of interest, you can use the diff and dry run flags to see what changes ansible
would make before it actually makes them. This means you can update the local
configuration and preview what impact this would have.

### 'diff' and 'dry-run' are useful for committing live server configuration

Consider this workflow:
- checkout a clean copy of master branch
- launch vagrant VM
- run ansible deployment
- SSH to vagrant VM and make changes
- run ansible deployment with 'diff' and 'dry-run' flag, the changes reported by
  ansible should effectively be a 'revert'
- update local configuration templates and tasks until no changes reported
- check in configuration changes and PR
- on merge to master, automation will pick up and will apply these updates to
  production, and other developers would pick up the same updates when they
  update their local copy of the code.

### Running with ansible

There is a wrapper script to run ansible, run it without args to get a usage text:

```bash
$ ./infrastructure/run_ansible
```

#### Tags

You can use the '--tags' arg to limit what is deployed. This can be used for
faster deployments, new tasks can be tagged with temporary tags to support
development. EG:

```bash
$ ./infrastructure/run_ansible --env vagrant --tags nginx,database,lobby
```

## Checking in '.orig' files

There are '.orig' files checked in alongside with the copies ansible uses to deploy.
These files are checked in for convenience to do diffs to see how we have deviated
from defaults. When modifying configuration on a remote server, first check in a copy
of the original configuration as a '.orig' file, then continue with checking in a new
version that is actually deployed.

