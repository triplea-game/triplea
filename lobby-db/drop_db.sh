#!/bin/bash

echo "drop database ta_users" | sudo -u postgres psql postgres
echo "create database ta_users" | sudo -u postgres psql postgres

