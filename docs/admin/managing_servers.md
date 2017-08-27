All TripleA related processes are started as the `triplea` user, so if you ever get an error because of insufficient permissions, make sure all the files are owned by the `triplea` user.


## Lobby

### Installation
https://github.com/triplea-game/lobby/blob/master/install_lobby

### Check status

Check lobby process is running (lobby port 3304 is a command line arg):
`ps -ef | grep 3304`


### Starting and Stopping
```
sudo service triplea-lobby start|stop|status|restart
```
Be advised, restarting the lobby quits all connections to all bots.
Even if the lobby restarts, the bots won't reconnect automatically, they will have to be restarted on their own.

_This command is currently restricted to users with full sudo rights._

## [The Dice server](https://github.com/triplea-game/dice-server)

Installed on the 'warclub' server

### Installation
TODO

### Check status
TODO

### Starting and Stopping

```
sudo service nginx restart
```

## [NodeBB Forums](https://forums.triplea-game.org)
- Runs on the "NJ" linode server
- NodeBB is deployed via git and dependencies are installed using npm. (TODO: add exact commands to deploy NodeBB)

### Installation
TODO

### Check status and Troubleshooting
If we ever run into problems with the forum and it keeps refusing to start, we need to do a couple things:
Before running any of those commands, we need to be in the correct working directory, `/opt/nodebb/` in our case.
We can do that by executing `cd /opt/nodebb/` in the beginning.
Run `./nodebb upgrade`, this should fix problems most of the time.
If it doesn't, make sure enough memory is available and the database is up and running. (`sudo service mongod status`)
If all of this doesn't help, open an issue at the [NodeBB repository](https://github.com/NodeBB/NodeBB) or create a topic in the [NodeBB community forum](https://community.nodebb.org).

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

_Note: Restarting fails sometimes if not enough resources e.g. memory are available. Always check for a successful restart and reasons for failures._

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


## TripleAWarClub Forum (Legacy)
The old tripleawarclub forum runs on the same server as the lobby, it is powered by XOOPS, written in PHP and uses MySQL as Database scheme. Because of more and more issues with XOOPS we decided to move to the NodeBB forum, which is much easier to maintain. To restart the WarClub forum, just restart nginx:
```
sudo service nginx restart
```
