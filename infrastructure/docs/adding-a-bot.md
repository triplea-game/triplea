# To add a bot:

- Add a [linode server](https://linode.com)
  - See [adding a linode](adding-a-linode.md) for configuration settings
- Create a [DNS entry](https://namecheap.com)
  - account > dashboard > "manage" button > "advanced DNS" > "create 'A' record"
  - once created, it takes some minutes for the record to become active
- Update [inventory file](/infrastructure/ansible/inventory/production) to include
  the new server
- Commit & submit for PR

After merge, Travis will do a production deployment and deploy software
to the new bot server and bring it up.
