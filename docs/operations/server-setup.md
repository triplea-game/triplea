# Server Setup (How To add a bot)

- Add a [linode server](https://linode.com)
- Create a [DNS entry](https://namecheap.com)
- Update inventory fileto include the new server
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

# Adding a linode

- Login to linode.
- Select the linode size

Update the following:

Choose a distribution (typically latest ubuntu):
![choose-distribution](https://user-images.githubusercontent.com/12397753/77502467-32df8000-6e18-11ea-984f-05c1561cdd4b.png)

Select a region, spread these out with highest concentration in US and EU.
Set a linode label & root password, follow naming pattern where number
jumps and we match the name to the region:

![select-region](https://user-images.githubusercontent.com/12397753/77502468-33781680-6e18-11ea-901e-904e9a2367e2.png)

Last, select the common SSH key (if none is displayed, ask your fellow admins
and add it by clicking the 'add key' button).

![select-label-and-password](https://user-images.githubusercontent.com/12397753/77502470-3410ad00-6e18-11ea-85b8-7bbb7e5edd67.png)


# Adding DNS Entries

Create a DNS entries on [namecheap.com > account > dashboard > "manage" button > "advanced DNS"](https://ap.www.namecheap.com/Domains/DomainControlPanel/triplea-game.org/advancedns)

All servers will have both an 'A' (IPv4 address) and 'AAAA' (IPv6 address) record. You can get both IPv4 and IPv6 addresses from the linode dashboard.

## Create an 'A' Record

![A Record](https://user-images.githubusercontent.com/12397753/82977167-e9392000-9f95-11ea-823f-ac599b222ebf.png)


## Create an 'AAAA' Record
![AAAA Record](https://user-images.githubusercontent.com/12397753/82977170-e9d1b680-9f95-11ea-8186-70e891ac7b5a.png)


## Create CAA Record

This is needed for lobby server or any server that will be serving https traffic.

![Screenshot from 2019-11-19 13-06-13](https://user-images.githubusercontent.com/12397753/69196411-48980e00-0ae3-11ea-9130-61e1fd5368b3.png)


