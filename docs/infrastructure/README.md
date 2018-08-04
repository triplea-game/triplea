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
- [Bot (game hosting) servers](https://github.com/triplea-game/triplea/blob/master/game-core/src/main/java/games/strategy/engine/framework/headlessGameServer/HeadlessGameServer.java)
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




