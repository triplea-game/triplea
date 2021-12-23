- [Typical Deployment Command](#typical-deployment-command)
- [Useful flags](#useful-flags)
- [Examples with Tags](#examples-with-tags)
  - [Deploy just bots](#deploy-just-bots)
  - [Deploy NGINX and CertBot](#deploy-nginx-and-certbot)
- [Example Prod Deployment](#example-prod-deployment)

# Ansible Commands

Deployments are run as the last step of travis builds, after artifacts
are uploaded to github releases. A utility script will download those artifacts
and place them in a location ansible can find them, then deployment to
prerelease will start which updates/upgrades/installs to the servers
defined in the prerelease inventory file.

- To run full stack locally, see the vagrant readme file in this same folder.
- To run deployment manually:

Assuming a vault password file named 'vault_password' is created in the same
folder and contains the ansible vault password, run:

```bash
./run_deployment [version_to_install] [ansible_args]
```

## Typical Deployment Command

```bash
./run_deployment 2.0.1000 -i ansible/inventory/prerelease
```

## Useful flags

- `-v`: Verbose output
- `-vvv`: Debug output
- `-vvvv`: SSL Debug output
- `--diff`: Shows differences in updated templates and files
- `-t`: Tags

## Examples with Tags

### Deploy just bots

```bash
./run_deployment 2.0.1000 -t bots -i ansible/inventory/prerelease
```

### Deploy NGINX and CertBot

```bash
./run_deployment 2.0.1000 -t nginx,certbot -i ansible/inventory/prerelease
```

## Example Prod Deployment

Production deployment is only a matter of specifying the production inventory file.

```bash
./run_deployment 2.0.1000 -i ansible/inventory/production
```
