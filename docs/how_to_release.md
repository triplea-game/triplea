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


## Change the version number

The version number exists in 1 place:
* https://github.com/triplea-game/triplea/blob/master/game_engine.properties


## About the TripleA Version Number:  
The last step before tagging is to bump the version number.  
This is non-trivial, and we have a very specific scheme we follow because it does affect a lot of things, such as loading old savegames.  
The TripleA Version number looks like “X.X.X.X”, such as “1.6.1.5” or “1.8.0.5”.  The numbers are called, in order: major.minor.point.micro  
If we bump any number except for ‘micro’, then all savegames from the previous version will be incompatible with the new version.  However, if we only bump the ‘micro’, then triplea will attempt to load savegames from any other version of triplea that has the same major.minor.point numbers.  
What this means, is that we need to decide on what to bump the version number to, based on what has changed in the engine.  
If our changes have rendered savegames unable to load, most likely because we changed one of the serialized data objects that are part of either the GameData object or any of the Delegate objects, then we MUST bump our version number by a major, minor, and/or point.  
UI changes, AI changes, rules and logic changes, and even API changes in most cases, usually do not cause savegame compatibility to break.  In this case, we should test extensively by grabbing various savegames from the previous versions (that have the same major.minor.point) and making sure they all load and play just fine (you can find lots of savegames in the forum at A&A.org).  Assuming savegames still load and play just fine, then we ONLY bump the ‘micro’ number.  
A further thing to note: We generally bump the version numbers once we make a savegame breaking change to the engine, even if we are not ready to release for another few months.  This is so that people who download and test the development branch or pre-release jars do not have issues with trying to load old savegames.  Development build should end with an EVEN micro, and full releases should end in an ODD micro.  
  
Last thing to note about backwards compatibility: We include a folder called ‘old’ that contains older versions of TripleA.  If TripleA tries to open a savegame that was made from a version of triplea with a different major.minor.point, it would normally not be able to do so, and so instead starts a new process directing the appropriate TripleA jar file in the old directory to open this savegame.  This also works when trying to join an ‘older’ game in the lobby; TripleA will tell the older jar to open and join that lobby game.  
Because micro number changes do not matter for savegames, version 1.7.0.5 of triplea can open 1.7.0.3 and 1.7.0.2 and 1.7.0.1 savegames (and of course can not open 1.7.1.x or 1.6.x.x, etc).  This means that we do not need to include every single triplea release in the ‘old’ directory, we only need the ‘latest’ triplea release for any given major.minor.point combination.  
So, to give a full example:  
Our last release (and current version number) was 1.8.0.7, but after several months, we decide to remove several fields in the ‘Unit’ object, and add several more that require non-null values at all times.  Adding fields usually does not break savegame compatibility as long as a null value is acceptable, but removing or renaming fields will definitely mess things up.  Because of this, we bump our version number up to 1.8.1.0 immediately (even).  We then find a copy of the previous version’s jar file and rename it to “triplea_1_8_0_7.jar” and put it into the ‘old’ directory and commit.  Several more months of coding go by, and we decide to release a beta (unstable) version for testing, with the idea that if no bugs are found we will quickly move to make a stable version (because we dislike beta versions that do not quickly become stable, because that splits the lobby users up into different versions for too long a time).  All our tests and steps above pass, so we then bump the version to 1.8.1.1 (odd) because it is a release.  

## Future Version Numbers


```
1.8.0.12 = test release compatible with 1.8.0.11
1.8.0.12(.build) = development build after 1.8.0.12 
1.8.0.13 = stable release compatible with 1.8.011
1.8.0.14 = test release compatible with 1.8.0.11
1.8.0.15 = stable release compatible with 1.8.011
1.8.0.16 = test release compatible with 1.8.0.11
1.8.0.17 = stable release compatible with 1.8.011
1.9.0(.0) = first release where save games are not compatible with 1.8.0.11
1.9.0(.build) = first development release after 1.9.0(.0)
1.9.1(.0) = next release still compatible with 1.9.0 save games
1.9.1(.build) = first development build after 1.9.1.0
1.9.2(.0) = next release compatible with 1.9.0
1.9.2(.build) = development release
1.9.3(.0) = next release compatible with 1.9.0
1.9.3(.build) = development release
1.10.0 = next release that breaks save games
1.10.1 = release compatible with 1.10.0
1.10.2 = release compatible with 1.10.0
1.11.0 = save games broken
1.11.1 = release compatible with 1.11.0
2(.0) = first release where saves are encoded in a cross-engine format
2(.build) = first development release after 2.0
3(.0) = next release 
3(.build) = development release
4(.0) = next release
4(.build) = development release
```
(http://github.com/triplea-game/triplea/issues/323#issuecomment-170802217 for background)


## Update the lobby bots and the lobby server jar.  
When uploading all these files to the tripleawarclub.org server, you must do so as the `triplea` user, otherwise you will need to chmod the files to be owned by the triplea user afterward.  It is easier and with fewer mistakes, to just all of the following as the triplea user.  
Unless you have changed the lobby’s API in any way, or have updated one of the libraries used by it (derby), then updating the lobby server jar is unnecessary.  If you need to update the lobby server, scp the lobby server zip file you created easier to the tripleawarclub.org server, under directory `/home/triplea/`.  The `triplea _x_x_x_x_server.zip` will contain a folder called `triplea_x_x_x_x` (ex: triplea_1_8_0_7), and you want to unpack that directly into the home directory.  It will come in as `triplea_x_x_x_x`, which is bad because we need to separate the bots from the server, so rename the file to `triplea_x_x_x_x_server`.  If you are running multiple lobby servers at the same time, you will need to edit the `run-server.sh` file and change the port number.  Start a new screen session with `screen` and then run the server with `./run-server.sh`.  For additional details on managing the server, see the bunker forum section at http://www.tripleawarclub.org/modules/newbb/viewforum.php?forum=14.  You should also start up triplea locally and make sure you can join the new lobby server successfully.  
Bots need to be updated to use the new version.  We have 2-4 bots running from tripleawarclub.org, and I usually have half of them running the latest stable, and half running the latest unstable, released versions (or all running the latest stable, if there is no unstable version out).  Scp the bot zip to tripleawarclub.org as the triplea user, to the triplea user’s home directory of `/home/triplea/`.  The `triplea_x_x_x_x_bots.zip`  will contain a folder called `triplea_x_x_x_x`, and you want to unpack that to the home directory, and then rename it to `triplea_x_x_x_x_bots` that way it does not conflict with the lobby server.  Remove the current run scripts from the new bots directory (ex: `rm -r triplea_1_8_0_7_bots/run-h*`), and then copy the current run scripts from the existing bots into the new bots directory (example: `cp -ravp triplea_1_8_0_5_bots/run-h* triplea_1_8_0_7_bots/`).  This is to keep the old bot’s settings (like their ports, their names, etc), because otherwise you’d have to update them yourself.  
Now, before starting any bots, go ahead and update the maps the bots use.  `cd /home/triplea/maps_repo` then `svn update`.  Maps might come in that have errors in them, which you won’t know about until you run the bots.  If you run the bots and the bots say there are errors parsing any maps, then we need to stop and quit the bot, then go delete those maps.  
After updating the maps, you should shutdown the old bots (or half of them, if we are unstable).  It is generally nice to let people know before we kill their game (without any way for them to save), so please log into the lobby server and alert anyone playing that they should save right away.  You can then use admin powers to remove the bots from the lobby or shut them down.  Run `screen -ls` to list all running screen sessions, and use `screen -r <pid>` to log into each screen session.  Warning: some of the screen sessions are the lobby/lobbies, and the rest are bots, so be careful which ones you shut down.  If the prompt inside the session begins with `>>>` then you are in a running lobby or bot.  If it is the default bash prompt, then nothing is currently running and you can `exit` this session to close it.  You can tell if your running session is a bot or lobby, by typing `help`; if the options are bot related then you are in a bot.  To close the bot, type `quit`, which will shut down the bot and exit out of the command script’s loop (then type `exit` to close the screen session).  
To start up new bots, change directory into the new bot server directory, then type `screen`, which will start a new screen session.  Run one of the bot scripts with `./run-headless-game-host<x>.sh` where <x> is the bot number.  The bot should start without issues, and appear in the lobby server shortly after.  If any log messages show errors with maps, go delete those maps and then restart the bot.  Using a local triplea session, join the lobby then join the bot, and start a game to confirm it works.  You can see more information about managing bots here: http://www.tripleawarclub.org/modules/newbb/viewforum.php?forum=14  
Once this is done, you have officially released TripleA!  


### Install of v1.9.0.0 lobby

```
 wget https://github.com/triplea-game/triplea/releases/download/1.9.0.0.2375/triplea-1.9.0.0.2375-server.zip
 unzip -d triplea_1_9_0_0_2375_lobby triplea-1.9.0.0.2375-server.zip
 cp triplea_1_8_0_5_lobby/run-server.sh triplea_1_9_0_0_2375_lobby/
 sed -i 's/port=3303/3304/' triplea_1_9_0_0_2375_lobby/run-server.sh
```


## Updating Desura and our partner sites:  
When TripleA is officially released, we need to update all of our partner’s websites, particularly Desura.  

- Desura (http://www.desura.com/games/triplea) is a ‘steam’ like service for smaller, indie-type, games.  Logging in as ‘tripleadevelopers@gmail.com’, you will go into the developer area (tab is called ‘publish’ I think), and follow the instructions.  This involves using some utility they give you to create a MCF file (basically a binary diff from previous versions).  You need to use the utility on an unpacked copy of the all-platforms-zip version of triplea.  Unfortunately, you will need to do it 4 times, once on a windows machine for the windows version, once on a mac for the mac version, and twice on a linux machine for the linux and linux64 versions.  After that, you make them become branch versions, and test them out using the Desura installer to make sure they work.  

- In addition to the ‘branch’ versions that install using the desura installer, you also need to upload ‘standalone releases’, one for each version of triplea we have (the same files we uploaded to sourceforge, etc).  
When this is all done, make a new post/news-item detailing that we have a new release.  
Partner sites are basically just other places to go download TripleA from, and are just file hosting sites.  Log into the following sites, and update their copies of triplea in all its flavors (windows, mac, linux/everyone):  
1.	http://www.freewarefiles.com/TripleA_program_56699.html  
2.	http://download.cnet.com/TripleA/3000-18516_4-75184098.html  


# Release Create Forum Post and latest_version.properties

## latest_version.properties file

The latest_version.properties file is a file that resides on the tripleawarclub.org web server (http://www.tripleawarclub.org/lobby/latest_version.properties) that controls what gets shown to users of older versions of triplea, when a new version of triplea is available.  This file is under version control of the TripleAMaps sourceforge svn, under `developer resources/triplea web/` (do not forget to commit changes).  The code that runs it can be found under games.strategy.engine.framework.GameRunner2.checkForLatestEngineVersionOut().  You can test out a copy of the properties file that you have uploaded by removing the checks to see if it has already been shown, and then iterate on the properties until they look the way you want them to.  The format is a .properties file format, which means each item on its own line, and no line breaks within an item.  The format is as follows:  
1.	`LATEST=1.8.0.5` -> This should be updated to whatever the newest version is.  This is the key that will tell people on older versions that it is time to upgrade.  
2.	`SHOW_FROM=1.6.1.4` -> This controls how far back the ‘changelog’ that is shown to the player will go.  I generally like to have it go back about 2 year’s worth of changes or so.  
3.	`LINK=http://triplea.sourceforge.net/` and `LINK_ALT=http://sourceforge.net/projects/tripleamaps/files/TripleA/` -> The links given to the player for where to go download the latest version.  
4.	`CHANGELOG=https://svn.code.sf.net/p/triplea/code/trunk/triplea/changelog.txt` -> Link for where to go to see the full changelog (needs to be updated to point to github).  
5.	` NOTES_1.8.0.5=<b style="font-size:120%">TripleA 1.8.0.5</b> (2015-Jan-01) <br /><b>- Bug Fixes: </b><br />Savegame size increasing exponentially bug fixed…….`  
  1.	The `NOTES_x.x.x.x=<html>` are the individual release notes for each of the versions, in html form.  Please follow the pattern that has already been established.  

The content for both will be quite similar, so I suggest doing both at the same time.  The main content is going to be a list of the new features, bugs, and changes.  To create this list, go take a look back through the changelog.  Any bug fixes that could have possibly affected the player should be listed.  Any changes to game rules or logic should be listed.  Any new major features should be listed.  Any new major refactorings or giant pieces of work that took forever to do should be listed.  Not everything in the changelog should be listed though, because we are only providing a summary to the end user, of things they care about.  


Once you are ready to release, you will upload the latest_version.properties file to the web servers.  The file is actually located on 2 different web servers, that way if one goes down we have a backup.  You will upload it to both the `triplea.sourceforge.net` web server, and also so `tripleawarclub.org` web server.`  Do this by scp’ing, or using win-scp, to upload it to `/srv/www/ tripleawarclub.org/public_html/lobby/` on the `tripleawarclub.org` web server, and by using SFTP to upload to `web.sourceforge.net` with your username and password, and then our project’s files will be in location `/home/project-web/t/tr/triplea/htdocs/`. 


## Creating the forum post
Avoid posting anything to the forum until we are ready to release.  
The forum post will be similar to past forum posts (example: http://tripleadev.1671093.n2.nabble.com/TripleA-1-8-0-5-Stable-has-been-released-tp7587305.html).  It is basically a notice to everyone that there is a new version, where to download it, and here are the cool features, bug fixes, and changes in it, etc.  Please use something close to the same format as previous posts.  

Also go create the forum thread on the following 3 websites: http://tripleadev.1671093.n2.nabble.com/Announcements-f1671668.html (pin it, and make sure only the latest stable (and unstable if applicable) are pinned), http://www.axisandallies.org/forums/index.php?board=53.0, and http://www.tripleawarclub.org/modules/newbb/viewforum.php?forum=1.  

## Update the website main page and news page

We need to update the website’s main page, and also the news page.  These two files are under source control of the TripleAMaps svn, under `developer resources/triplea web/`.  The two files are `TripleA` and `News`.  Follow the format provided and change the links and text to point to new version you are releasing.  If you are releasing an unstable version, please do not delete the links and text to the stable version, as we want to provide both, so just make a copy under the stable version and change that.  You will then need to upload these to the sourceforge web server for our main site.  To do so, use SFTP to upload to `web.sourceforge.net` with your username and password, and then our project’s files will be in location `/home/project-web/t/tr/triplea/htdocs/`.  Remember to commit changes back to the repo.  

## Point new versions to the lobby using the server_x.x.x.x.properties file:  

We control which versions access which online lobbies, using a server_x.x.x.x.properties file (example: `server_1.8.0.7.properties`).  These files are under source control of the TripleAMaps svn, under `developer resources/triplea web/lobby/`.  We generally only allow access to the lobby to the latest stable, unstable, and development versions.  In the past, we actually had different lobbies for each of these, however they were unified into a single lobby in late 2013, and games just show which version they are.  (If we ever break any of the API’s for the lobby, then we will need to have separate lobbies again, but hopefully we never have to do that.)  The fields for the server properties file are:  
1.	`HOST=<ip or domain>` -> This is the server that hosts our lobby.  
2.	`PORT=3303` -> This is the port which our lobby server is using.  If we have separate lobbies, they will be on different ports, but otherwise this number should always be 3303.  
3.	`MESSAGE=\nVeqryn welcomes you to the new …….` -> This is the message that will appear to users in the chat panel when they first join the lobby.  You will need to update this to reflect any changed version number and links.  
4.	`ERROR_MESSAGE=Please update by downloading newest version of TripleA: http://sourceforge.net/projects/triplea/files/` -> This is the error message that will be shown to users when they try to join the lobby with this version of triplea (based on the name of the file).  IF this is not empty, then the message will be shown.  IF it is empty, they will be allowed to join.  This is how we control access to the lobby, and keep everyone in the lobby using the same version (or couple versions) of triplea.  
Once the new server property file is ready, you will need to upload it to both web servers.  The files are actually located on 2 different web servers, that way if one goes down we have a backup.  You will upload it to both the `triplea.sourceforge.net` web server, and also so `tripleawarclub.org` web server.`  Do this by scp’ing, or using win-scp, to upload it to `/srv/www/ tripleawarclub.org/public_html/lobby/` on the `tripleawarclub.org` web server, and by using SFTP to upload to `web.sourceforge.net` with your username and password, and then our project’s files will be in location `/home/project-web/t/tr/triplea/htdocs/`.  
We generally give 2-3 weeks for people on an old stable, or old unstable, to migrate to the new version, before we turn off access to the old version.    




