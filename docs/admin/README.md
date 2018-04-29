# How to Release

## Game Engine Release
- Verify the release notes: https://github.com/triplea-game/triplea-game.github.io/blob/master/release_notes.md
   - Add link to full list of merged PRs included in release: https://github.com/triplea-game/triplea-game.github.io/blob/master/.github/generate_release_changes_url
- Mark the target release version as latest: https://github.com/triplea-game/triplea/releases

## Major releases
- Can send new game clients to a new lobby: https://github.com/triplea-game/triplea/blob/master/lobby_server.yaml
- Change the version number in the travis build file: https://github.com/triplea-game/triplea/blob/master/.travis.yml
- Trigger game client notifications: https://github.com/triplea-game/triplea/blob/master/latest_version.properties
- Update partner sites:  
  - http://www.freewarefiles.com/TripleA_program_56699.html  
  - http://download.cnet.com/TripleA/3000-18516_4-75184098.html  
- Post to forums:
  - https://forums.triplea-game.org/category/1/announcements
  - http://www.axisandallies.org/forums/index.php?board=53.0

# Server Ops

## Lobby

### Installation
https://github.com/triplea-game/lobby/blob/master/install_lobby


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


# Bot Account

Bot Account: [github.com/tripleabuilderbot](https://github.com/tripleabuilderbot)

An admin owned account used for automated build tasks that require repository write access.

## Personal Access Keys

![Tokens](https://cloud.githubusercontent.com/assets/12397753/26811743/822517d6-4a28-11e7-8342-ef4826e834b9.png)

- *push_tags*: 
  - *Description*: Write permission to TripleA repo for creating a new tag on each release.
      The tags are primarily for convenience, so we can relatively easily checkout a specific
      version that we released. This is also a carry-over of the process TripleA used when
      hosted in SVN.
  - *Location*: environment variable
  - *Usage*: [push_tags script](https://github.com/triplea-game/triplea/blob/master/.travis/push_tag#L13)
- *push_maps*
  - *Description*: Write-Access to TripleA github.io website repo to add map description files
  - *Location*: environment variable
  - *Usage*: [push_maps script](https://github.com/triplea-game/triplea/blob/master/.travis/push_maps#L8)
- *automatic releases*
  - *Description*: This key allows Travis to push to github releases. It is set up by the
      Travis ruby set up program when first configuring travis with TripleA.
  - *Stored Location*: encrypted in the travis.yml file
  - *Usage*: [travis.yml](https://github.com/triplea-game/triplea/blob/master/.travis.yml#L32)


## Regenerating Travis Environment Variables:

Can be done through the Travis UI. Note they are write-once, so they just need to be deleted and re-created with known values.

The config can be found here (You must be logged in as the bot or with admin/write-access to TripleA): [travis-ci.org/triplea-game/triplea/settings](https://travis-ci.org/triplea-game/triplea/settings)
![Travis](https://cloud.githubusercontent.com/assets/12397753/26811735/6e69c5de-4a28-11e7-8996-49338f428349.png)
