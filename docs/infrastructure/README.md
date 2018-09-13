# Systems Administration

## Tasks and scope of system admin
- Install triplea applications to prod servers
- Get applications running, open up firewall ports and start services
- Application Log Management
- Version upgrades

## Applications Owned by System Admin

[Full list](https://github.com/triplea-game/infrastructure/tree/master/roles) is in infrastructure 'roles'. A 'role' 
loosely maps to an app.

### User-Facing TripleA Apps
- [Lobby](https://github.com/triplea-game/triplea/tree/master/lobby)
- Dice Roller (aka: marti)
- [Bot (game hosting) servers](https://github.com/triplea-game/triplea/tree/master/game-headless)
- [NodeBB Forums](https://forums.triplea-game.org) (nodeBB)

### Support Apps
- Grafana (TODO: add link): application and system metrics graphing
- Prometheus (TODO: add link): metrics aggregation and alarming
- PaperTrail (TODO: add link): third party app for log aggregation, all servers logs are sent there real time
and can be viewed, filtered and searched with their web-app. (note: short log retention, ~3 days)


## Infrastructure Project and TripleA System Admin Automation

### Philosophy of Infrastructure as Code

The idea here is to update production servers you do not log in to them, instead configuration files are updated
and the servers will read these configs and then update themselves. This has a lot of benefit, it ensures that
server setup is scripted (which is even better than documentation), and it keeps a track record so a team can
coordinate and know what has happened to the servers. This keeps the servers in a shared 'known state'.

Updates to the infrastructure configuration can also be reviewed by the team before they are pushed out, this helps
reduce mistakes and increases communication of what is changing. It also helps ensures that things are changed 
correctly. Last, we have a branch strategy where the latest changes always go to the prerelease environment so we
can make things are good before merging those changes up to the 'prod' branch which would update production servers.


### Infrastructure Project, How it Works

[Infrastructure Project](https://github.com/triplea-game/infrastructure) is an in-house set of shell scripts that have 
a similar structure to [ansible](https://www.ansible.com/). The idea is that the in-house version is pretty bare-bones,
simple to follow. 

Most of the system administration is handled by checking in updates to those files. Servers are grouped into
'[roles](https://github.com/triplea-game/infrastructure/tree/master/roles)' which are mapped to specific servers
in '[host_control.sh](https://github.com/triplea-game/infrastructure/tree/master/roles)'.

Every server will re-clone the infrastructure project every 5 minutes and executes a top level entry script, 
'[system_control.sh](https://github.com/triplea-game/infrastructure/blob/master/root/system_control.sh)', that
does common configuration and updates for each server. After this common step each server will do the role
specific install actions, for example bots will run 
[bots.sh](https://github.com/triplea-game/infrastructure/blob/master/roles/bot/bot.sh) that will install or
update the bot server. The version number is a parameter, along with all other role specific configuration,
so once we have the update script installed on a server we really only need to control it by updating the 
configuration in [host_control.sh](https://github.com/triplea-game/infrastructure/tree/master/roles).




# [NodeBB Forums](https://forums.triplea-game.org)
- Runs on the "NJ" linode server
- NodeBB is deployed via git and dependencies are installed using npm. (TODO: add exact commands to deploy NodeBB)

### Check status and Troubleshooting

If forum is crashed and refuses to start:
```bash
cd /opt/nodebb/
./nodebb upgrade
```
This should fix problems most of the time.
If it doesn't, make sure enough memory is available and the database is up and running.
 
 ```
 sudo service mongod status
 ```
 
If all of this doesn't help, open an issue at the [NodeBB repository](https://github.com/NodeBB/NodeBB) or create a 
topic in the [NodeBB community forum](https://community.nodebb.org).

#### Log files
Get last 50 nodebb log lines:
`sudo journalctl | grep "nodebb" | tail -n 50`.

NodeBB uses stdout to log everything, stdout is then attached to journalctl.
jornalctl deletes log files after a couple weeks in order to save space.


### Restarting
- When we have an admin account on the forum, we can restart it using the webinterface.
- If this webinterface is not available because of a crash or a bad configuration file, it can be done by hand:

```
sudo service nodebb restart
sudo service nginx restart
```

_Note: Restarting fails sometimes if not enough resources e.g. memory are available. Always check for a successful 
restart and reasons for failures._

### Updating
Updating is not always the same, depending on the branch policy of NodeBB.
You should preferably execute all of the following commands as the `nodebb` user, or make sure all the files belong to the `nodebb` user after upgrading.
```
# First of all shut down the forum.
sudo service nodebb stop

# Make sure you are in the correct working directory
cd /opt/nodebb/

# Sometimes the release branch is changed. In this case we need to do a checkout:
# new_branch_name refers to the branch name of the current release of the NodeBB repository.
# It can be found here: https://github.com/NodeBB/NodeBB
git fetch --all
git checkout <new_branch_name>

# All of the following commands need to always be executed:

# Pull the latest changes from the remote repository.
git pull
# Install the latest dependencies.
./nodebb upgrade

# Last but definitely not least restart the forum.
sudo service nodebb start
```

