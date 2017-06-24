
## Manual Release Testing

- Can download maps, and can see and load both maps that come with triplea and downloaded maps.
- Launch an AI only game and let some AI players go at for a dozen rounds, verify no errors
- Join a bot, play through a combat
- Compatibility checks
  - save games
  - network play
  - XML parsing
  
- Local host + client
  - This means start up two copies of triplea.  In one copy you click ‘host networked game’ 
and start hosting.  In the other copy you click ‘connect to networked game’ and join.  The client should choose the 
FIRST player in the list (this is to test that the delegates are sending data correctly).  
Start the game and play a round against yourself, making sure everything looks good.  

- Play by Email, and Play by Forum  
  - You can test both at once by setting up a game against yourself that has both settings enabled.  You will probably need to add two email addresses for this to work.  I also recommend testing the play by forum against both AxisAndAllies.org and TripleAWarClub.org, and also testing the play by email against both a gmail and a hotmail email account.  


## Tasks:

- Verify the release notes: https://github.com/triplea-game/triplea-game.github.io/blob/master/release_notes.md
   - Should have just about all significant changes and bug fixes
- Change the version number in the game_engine.properties file: https://github.com/triplea-game/triplea/blob/master/game_engine.properties
- Update the lobby bots and the lobby server: https://github.com/triplea-game/lobby
- Bump download version on the website: https://github.com/triplea-game/triplea-game.github.io/blob/master/_config.yml
- Mark latest as a release: https://github.com/triplea-game/triplea/releases
- Update partner sites:  
  - http://www.freewarefiles.com/TripleA_program_56699.html  
  - http://download.cnet.com/TripleA/3000-18516_4-75184098.html  
- Post to forums:
  - https://forums.triplea-game.org/category/1/announcements
  - http://www.axisandallies.org/forums/index.php?board=53.0


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

