# Ansible Public Key

Ansible needs to communicate to target servers via ssh. Locally we
have a private key that is encrypted and decrypted when ansible runs
(decryption is via ansible vault). To enable this, we need the ansible
public key to be deployed to the target server under the root users
'authorized_keys' file.

The installation of a public key to root user can be done during linode
creation from the lindoe web UI. Add this public key to your linode account
 profile (via the linode website):

> ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIBdU9dU02UR5MCutULVgpdT1mN6wjJOKL8sW1/ZZkdym ansible-public-key

Then, when creating a new linode, select that public key and it will added
to the root user 'authorized_keys' file.
