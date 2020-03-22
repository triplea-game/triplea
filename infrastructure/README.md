# Infrastructure Project

Home to infrastructure deployment, notably done with a technology called [ansible](https://www.ansible.com)

Infrastructure deployment refers to how we set up and deploy software to test and production servers.

For more info on how TripleA deploys with ansible, see: [Ansible Overview](./docs/ansible-overview.md)

Notably ansible is designed to be idempotent, we run deployments to prerelease and production on every merge
to master. This is triggered from [travis](https://travis-ci.org/github/triplea-game/triplea) which builds the TripleA
master branch after each PR is merged.

Each ansible 'role' can be thought of as an application that is deployed. Roles should be pretty atomic and
granular so that we can easily configure and re-use them between different hosts. Each [role folder](./ansible/roles)
should have a README.md file that describes what is deployed by that role.

See the [docs folder](./docs) for various documentation, particularly how to use a  
[local vagrant machine](./docs/local-development-with-vagrant.md) to do deployment automation development locally 
before needing to merge.


