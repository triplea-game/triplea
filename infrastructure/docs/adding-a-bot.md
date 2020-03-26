# To add a bot

- Add a [linode server](https://linode.com)
  - See [adding a linode](adding-a-linode.md) for configuration settings
- Create a [DNS entry](https://namecheap.com)
  - account > dashboard > "manage" button > "advanced DNS" > "create 'A' record"
  - once created, it takes some minutes for the record to become active
- Update [inventory file](/infrastructure/ansible/inventory/production) to include
  the new server
- Commit & submit for PR


One day, Travis will do a production deployment automatically
on merge and deploy software to the new bot server and bring it up.


For now, run ansible deployment by-hand:

```
cd --------/triplea/infrastructure

# create a file called 'vault_password' containing the
# ansible vault password

ansible-vault view \
     --vault-password-file=vault_password \
     ansible_ssh_key.ed25519 \
  | ssh-add -

ansible-playbook \
   --vault-password-file vault_password \
   -i ansible/inventory/production \
   ansible/site.yml \
   --limit=[BOT-DNS-NAME] \
   -t bots \
   -v
```

