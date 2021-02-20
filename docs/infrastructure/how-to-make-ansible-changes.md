# Updating ansible deployment script

Ansible files are in '/infrastructure/ansible'

After updating them, use vagrant to verify. To
start a vagrant VM and deploy changes to it,
run:

```
cd /infrastructure
vagrant up
./run_ansible_vagrant
vagrant ssh
# verify changes
```

The `run_ansible_vagrant` script accepts ansible
flags, such as '-t' to run specific tags. EG:
```
./run_ansible_vagrant -t lobby_server
```

