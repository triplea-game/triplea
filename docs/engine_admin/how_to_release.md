---
layout: longpage
title: How to Release
permalink: /dev_docs/admin/how_to_release
---


## Release Testing
1. Test the following situations at a bare minimum (our unit tests cover virtually nothing):  
 1.	Can download maps, and can see and load both maps that come with triplea and downloaded maps.  
 2.	Single player vs each different AI.  Just a single round or two is enough.  
   1.	Save the game midway through.  Try to load the game later, and make sure it worked.  
 3.	Online multiplayer through the lobby vs someone else with the same version of the engine.  
 4.	Local host + client  
   1.	This means start up two copies of triplea.  In one copy you click ‘host networked game’ and start hosting.  In the other copy you click ‘connect to networked game’ and join.  The client should choose the FIRST player in the list (this is to test that the delegates are sending data correctly).  Start the game and play a round against yourself, making sure everything looks good.  
2. Play by Email, and Play by Forum  
  1.	You can test both at once by setting up a game against yourself that has both settings enabled.  You will probably need to add two email addresses for this to work.  I also recommend testing the play by forum against both AxisAndAllies.org and TripleAWarClub.org, and also testing the play by email against both a gmail and a hotmail email account.  
3.	Make sure the changelog is filled with just about all significant changes and bug fixes.  
4.	Make sure a ‘beta’ copy has been out in the wild for at least 1-2 weeks, with no bugs reported against it and no changes to it since it was out.  
  1.	This is especially true about “stable” releases.  When you think you are ready to take a beta (unstable) release, and re-release it as stable, make sure that the beta version had a full release and has been played in the online lobby for at least a couple weeks, with no bugs reported against it and no non-cosmetic commits to it, before you re-release it as a stable (and yes, you will bump the version number in order not to confuse people).  


## Change the version number in the game_engine.properties file
* https://github.com/triplea-game/triplea/blob/master/game_engine.properties


## About the TripleA Version Number:  
See: https://github.com/triplea-game/triplea/blob/master/docs/admin/version_numbers.md


## Update the lobby bots and the lobby server jar.  

Lobby install script is here: https://github.com/triplea-game/lobby
It is cloned on the triplea account on the server @ /home/triplea/lobby-scripts

To install, from the server with an account that has 'sudo -u triplea' access, run:
```
@tripleawarclub:/home/triplea/lobby-scripts$ git pull --rebase
@tripleawarclub:/home/triplea/lobby-scripts$ sudo -u triplea ./install_lobby 1.9.0.0.3096 ../lobby_1_9/
@tripleawarclub:/home/triplea/lobby-scripts$ sudo -u triplea ./install_bot 1.9.0.0.3096 ../bots_1_9/
@tripleawarclub:/home/triplea/lobby-scripts$ cd ../lobby_1.9.0.0.3096/
@tripleawarclub:/home/triplea/lobby_1_9/lobby_1.9.0.0.3096$ ps -ef | grep 3304
triplea   8095  8093  0 Sep05 ?        00:02:15 java -server -Xmx256m -classpath bin/triplea.jar:lib/derby-10.10.1.1.jar -Dtriplea.lobby.port=3304 games.strategy.engine.lobby.server.LobbyServer
@tripleawarclub:/home/triplea/lobby_1_9/lobby_1.9.0.0.3096$ sudo kill 8095
@tripleawarclub:/home/triplea/lobby_1_9/lobby_1.9.0.0.3096$ sudo -u triplea cp -r ../lobby_1.9.0.0.3074/derby_db/ ./
@tripleawarclub:/home/triplea/lobby_1_9/lobby_1.9.0.0.3096$ sudo chmod +x run_lobby
@tripleawarclub:/home/triplea/lobby_1_9/lobby_1.9.0.0.3096$ sudo -u triplea nohup ./run_lobby &
```

If you are running multiple lobby servers at the same time, you will need to edit the `run_lobby` file and change the port number (if this becomes common, we'll want to include it is a param).  Also assumed is that the port is enabled in ufw.


Download all maps for a robot, and run lobby bots:
```
@tripleawarclub:/home/triplea/lobby-scripts$ sudo -u triplea mkdir maps
@tripleawarclub:/home/triplea/lobby-scripts$ cd maps
## downloads all maps to the current folder
@tripleawarclub:/home/triplea/lobby-scripts/maps$ sudo -u triplea ../download_all_maps
@tripleawarclub:/home/triplea/lobby-scripts/maps$ cd ../
@tripleawarclub:/home/triplea/lobby-scripts$ sudo rm -rf ../bots_1_9/maps/
@tripleawarclub:/home/triplea/lobby-scripts$ sudo -u triplea mv maps/ ../bots_1_9/

## now start bots
@tripleawarclub:/home/triplea/lobby-scripts$ cd ../bots_1_9/bot_1.9.0.0.3096/

## launches a bot on port 4001, numbered: 1
@tripleawarclub:/home/triplea/bots_1_9/bot_1.9.0.0.3096$ sudo -u triplea nohup ./run_bot 4001 1 &

## view the logs after launching our bot, check for map errors or startup errors
@tripleawarclub:/home/triplea/bots_1_9/bot_1.9.0.0.3096$ tail -f logs/headless-game-server-Bot1_WarClub-log0.txt
```

If you run the bots and the bots say there are errors parsing any maps, then we need to stop and quit the bot, then go delete those maps.  


## Bump download version on the website
Update here: https://github.com/triplea-game/triplea-game.github.io/blob/master/_config.yml


## Updating Desura and our partner sites:  

When TripleA is officially released, we need to update all of our partner’s websites, particularly Desura.  

- Desura (http://www.desura.com/games/triplea) is a ‘steam’ like service for smaller, indie-type, games.  Logging in as ‘tripleadevelopers@gmail.com’, you will go into the developer area (tab is called ‘publish’ I think), and follow the instructions.  This involves using some utility they give you to create a MCF file (basically a binary diff from previous versions).  You need to use the utility on an unpacked copy of the all-platforms-zip version of triplea.  Unfortunately, you will need to do it 4 times, once on a windows machine for the windows version, once on a mac for the mac version, and twice on a linux machine for the linux and linux64 versions.  After that, you make them become branch versions, and test them out using the Desura installer to make sure they work.  

- In addition to the ‘branch’ versions that install using the desura installer, you also need to upload ‘standalone releases’, one for each version of triplea we have (the same files we uploaded to sourceforge, etc).  
When this is all done, make a new post/news-item detailing that we have a new release.  
Partner sites are basically just other places to go download TripleA from, and are just file hosting sites.  Log into the following sites, and update their copies of triplea in all its flavors (windows, mac, linux/everyone):  
1.	http://www.freewarefiles.com/TripleA_program_56699.html  
2.	http://download.cnet.com/TripleA/3000-18516_4-75184098.html  


# Create Forum Post and latest_version.properties

## latest_version.properties file
Update:
- Update: https://github.com/triplea-game/triplea/blob/master/latest_version.properties
- Lobby config file to update: https://github.com/triplea-game/triplea/blob/master/lobby_server.yaml

TODO: the above fields are maybe out of date, particularly the change log parts:
1.	`LATEST=1.8.0.5` -> This should be updated to whatever the newest version is.  This is the key that will tell people on older versions that it is time to upgrade.  
2.	`SHOW_FROM=1.6.1.4` -> This controls how far back the ‘changelog’ that is shown to the player will go.  I generally like to have it go back about 2 year’s worth of changes or so.  
3.	`LINK=https://triplea.sourceforge.net/` and `LINK_ALT=https://sourceforge.net/projects/tripleamaps/files/TripleA/` -> The links given to the player for where to go download the latest version.  
4.	`CHANGELOG=https://svn.code.sf.net/p/triplea/code/trunk/triplea/changelog.txt` -> Link for where to go to see the full changelog (needs to be updated to point to github).  
5.	` NOTES_1.8.0.5=<b style="font-size:120%">TripleA 1.8.0.5</b> (2015-Jan-01) <br><b>- Bug Fixes: </b><br>Savegame size increasing exponentially bug fixed…….`  
  1.	The `NOTES_x.x.x.x=<html>` are the individual release notes for each of the versions, in html form.  Please follow the pattern that has already been established.  

Ensure the release notes are updated: https://github.com/triplea-game/triplea-game.github.io/blob/master/release_notes.md


## Creating the forum post
Post to:
- http://www.tripleawarclub.org/modules/newbb/viewforum.php?forum=1.  
- http://www.axisandallies.org/forums/index.php?board=53.0

Avoid posting anything to the forum until we are ready to release.  
The forum post will be similar to past forum posts (example: http://tripleadev.1671093.n2.nabble.com/TripleA-1-8-0-5-Stable-has-been-released-tp7587305.html).  It is basically a notice to everyone that there is a new version, where to download it, and here are the cool features, bug fixes, and changes in it, etc.  Please use something close to the same format as previous posts.  


## Update the website main page and news page


### OLD SOURCEFORGE INSTRUCTIONS
We need to update the website’s main page, and also the news page.  These two files are under source control of the TripleAMaps svn, under `developer resources/triplea web/`.  The two files are `TripleA` and `News`.  Follow the format provided and change the links and text to point to new version you are releasing.  If you are releasing an unstable version, please do not delete the links and text to the stable version, as we want to provide both, so just make a copy under the stable version and change that.  You will then need to upload these to the sourceforge web server for our main site.  To do so, use SFTP to upload to `web.sourceforge.net` with your username and password, and then our project’s files will be in location `/home/project-web/t/tr/triplea/htdocs/`.  Remember to commit changes back to the repo.  




Once the above is done, you have officially released TripleA!  
