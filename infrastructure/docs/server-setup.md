# Adding a New Server

## Requirements

- You have an account in linode
- You have been added to the TripleA org in linode
- You have an account on namecheap
- You have been added to the TripleA org in namecheap
- You have access to the administrator secrets document

## General Steps

1. Add a [linode server](https://linode.com)
1. Create a [DNS entry](https://namecheap.com)
1. Add server to inventory, commit & submit PR

# Add a linode server

Login to linode and create a server.

Typical configuration:

- nanode ($5/month)
- Ubuntu latest LTS
- Region, somewhere in US or Europe
- Set linode label (follow naming convention)
- Set root password per admin secrets document 
- Select to add a SSH key


# Adding DNS Entries

Create a DNS entries on [namecheap.com > account > dashboard > "manage" button > "advanced DNS"
](https://ap.www.namecheap.com/Domains/DomainControlPanel/triplea-game.org/advancedns)

All servers will have both an 'A' (IPv4 address) and 'AAAA' (IPv6 address) record. 
You can get both IPv4 and IPv6 addresses from the linode dashboard.

## Create an 'A' Record

![A Record](https://user-images.githubusercontent.com/12397753/82977167-e9392000-9f95-11ea-823f-ac599b222ebf.png)

## Create an 'AAAA' Record
![AAAA Record](https://user-images.githubusercontent.com/12397753/82977170-e9d1b680-9f95-11ea-8186-70e891ac7b5a.png)

## Create CAA Record

This is needed for lobby server or any server that will be serving https traffic.

![Screenshot from 2019-11-19 13-06-13](https://user-images.githubusercontent.com/12397753/69196411-48980e00-0ae3-11ea-9130-61e1fd5368b3.png)

## Update inventory file

- Add the server to [inventory configuration](/infrastructure/ansible/inventory)
- Commit the changes and submit for a PR. After the PR is merged, travis will run a deployment
  automatically deploying all needed configurations to the server. 
