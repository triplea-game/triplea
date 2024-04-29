#!/bin/bash

# This script will download all maps to all bots.
# Requires no args to run, intended to be run from Github Actions (via the web interface of Github)

set -eu

scriptDir="$(dirname "$0")"
rootDir="$scriptDir/.."

VAULT_PASSWORD_FILE="$rootDir/infrastructure/vault_password"
if [ ! -f "$VAULT_PASSWORD_FILE" ]; then
  echo "Deployments to production environment requires vault password file: $VAULT_PASSWORD_FILE"
  exit 1
fi

set -x
ANSIBLE_CONFIG="$scriptDir/ansible.cfg" ansible-playbook \
   --inventory "$scriptDir/ansible/inventory/production" --limit botHosts  \
   --vault-password-file "$VAULT_PASSWORD_FILE" \
   --tags update_maps \
   "$scriptDir/ansible/site.yml"
set +x
