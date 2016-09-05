# Map Making Documentation

## Requirements
* A github account

## Simple XML Updates
* Find the github map repo of the map you would like to update: https://github.com/triplea-maps
* Click through to the XML file
* Find the pencil icon, that let's you edit the file.
* When saving, github will help prompt you, you will create a fork which is a remote copy that you own. The UI will guide you to creating this, saving (called commit), and then you can request your copy be merged into live master copy (called a pull request). You'll be able to do all these steps from the UI that let's you save file edits

## Adding or replacing files
* The github UI will allow you to do this. To do it in bulk you may need to do some git (sourcetree, git for windows and other GUI tools can help)

## New Map Development 
* The key is getting the folder structure right.

* You can develop your map directly from the "downloadedFolders" map and the game engine will pick it up. Note, you can change this folder location in-game via the settings window. For example, your "downloadedFolders" might look like this:  ![above_the_project](https://cloud.githubusercontent.com/assets/12397753/17640925/f50e1876-60c0-11e6-96d8-483f0a84f389.png)

* In the above picture, "big_world_2" is the folder we are working on, after creating a similar folder, the next level should contain a "map" folder. Any other files at this level are support. A typical config will look like this:
![bare_bones_top_level](https://cloud.githubusercontent.com/assets/12397753/17640936/30528e44-60c1-11e6-815e-e03c395a26b5.png)
or like this:
![top_level](https://cloud.githubusercontent.com/assets/12397753/17640928/f9269118-60c0-11e6-84b5-63a0153ed4fb.png)

** Everything in the "map" folder will be packaged with the map zip when it is downloaded from within a game.

* The last level contains the standard map folders and files, should look something like this: ![3rd_level](https://cloud.githubusercontent.com/assets/12397753/17640896/010137cc-60c0-11e6-8f02-4700c709ab66.png)


### Posting to Github
* Create a github account
* Create a repository
* Upload your map folder to it
** If the upload size is too large, do it a few folders at a time
** Note, do not upload any zips, they cannot be modified easily, and it is unnecessary, the game engine can read unzipped map projects just fine

* To make the map official, it needs to be in the triplea-map organization, and needs to be included in the download index file.
** You can change ownership of a github repo, open a ticket here for guidance: https://github.com/triplea-maps/Project/issues
** The download index file is here: https://github.com/triplea-maps/Project/blob/master/production_config/triplea_maps.yaml


### Testing

The game engine can load maps directly. For instructions, see the 'adding new map' instructions above, and/or: https://github.com/triplea-maps/Project


# Legacy Docs
The docs below are a bit legacy.. they need to be cleaned up/ simplified.. But still here in case it helps anyone


## How to Request Map XML Updates
* This can be done using just your web browser:
  * Find the github map repo of the map you would like to update: https://github.com/triplea-maps
  * Click through the maps/games folder
  * Now you can edit the XML file, click the file name, and follow these instructions: http://help.github.com/articles/editing-files-in-your-repository
* This will lead to the creation of a pull request. Map admins and or map owners will review the change and will then either comment on it requesting clarification or updates, or they will merge it.

## Map Releases and Automated Zipping
* We are integrated with Travis that will watch map repositories for updates. Whenever a map repository is updated, Travis will kick and will version the map, and publish a map zip to the '/releases' section of the map repo. For example: https://github.com/triplea-maps/age_of_tribes/releases
* You should find that you no longer have to do any map zipping or unzipping.
 

## Github repo change ownership option

Create a repository in github. Use the same general layout as the other map repositories. Stage your map repo from the 'downloadedMaps' folder, here the game engine will pick it up and you can test it out and update it. Check it all in, and push it to your repository. Then in the github repository settings, look for the 'change ownership' option, and you can request for the 'triplea-maps' org to take ownership. After ownership has been transferred we would add in a ".travis" file to enable automated zipping, and we would update: https://github.com/triplea-maps/Project/blob/master/production_config/triplea_maps.yaml, to make the map live


## Local Map Testing

* Some minimal Git knowledge allows the process to be self-service
  * You need a github account: http://github.com/account
  * You need a git client tool on your local machine. An easy one that gives you a graphical user interface is [Git for windows](https://git-scm.com/download/win)
  * - [Setup your local Git email and username](http://github.com/triplea-maps/Project/wiki/Map-Makers:-Git-Username-and-Email-Setup)



### Clone a map repo
Navigate to your home folder 'triplea/downloadedMaps'. You can change that location in-game in the settings window. You may clone map repositories to this location, and the game engine will pick up and load the map. Note, clear out an existing .zip file of the same file. It can be advisable to use a new folder somewhere to hold your clones, and use the game settings to use that folder for maps.

Next, actually clone the map repo:

* Find the map repo, https://github.com/triplea-maps/
  * Fork the map if you are not an owner/admin. To fork, click through to the repo and simply click the 'fork' button
* Follow these steps to clone: https://help.github.com/articles/cloning-a-repository/#platform-windows, until it says "open git bash". To use the Git for windows user interface instead of command line, then go to section 3 of this: http://www.thegeekstuff.com/2012/02/git-for-windows/
* Note 'git bash' is git command line. You can open a git interface on any folder in windows by right clicking it and selecting the 'git gui from here' option. 



### Modify files
Now you can modify the maps files. You can add/remove, or modify any of the files and git will track the changes locally. 

### Upload Changes
Using git for windows you can add new files to "staged", you will then commit them, and then push. 
Stay tuned for some more help notes here.. Please post an issue if you find any good links/documentations: github.com/triplea-maps/Project/issues/new


## Getting More Help
- Ask questions here: github.com/triplea-maps/Project/issues/new
- Or Jump on gitter: https://gitter.im/triplea-maps/Project
  - You can chat there about maps, map making. And also you'll hear announcements as well, and the general community chatter. If you drop off for some time, for a few weeks or a few months, then you'll need to review the docs a bit to see what you missed. At least, that is the intent. So if you have any questions, where you want to reach map makers and the map admins, you can ask them on gitter, and chances are decent they'll be answered within hours or a day. 


## How are maps included with the game?

* Map description and download links are pulled from: https://github.com/triplea-maps/Project/blob/master/production_config/triplea_maps.yaml
* Please submit a PR to that file to include/update maps.


## General note - To Update a PR
* If you would like to update your pull request, here the steps (in this example we will assume your github username is "Mappy"):
1. Head to your fork of triplea: https://github.com/Mappy/triplea
2. Click the branches button to view your branches: https://github.com/Mappy/triplea/branches
3. Find the branch which you used to create the PR: https://github.com/Mappy/triplea/tree/patch-1
4. Update the triplea_yaml file as normal, click it and use the pencil icon,
*profit* The update/commit of the triplea_yaml file will automatically update the PR



## Was this confusing?
Please help clarify these notes:
* https://github.com/triplea-maps/Project/edit/master/MapMakingTools/README.md
* Pencil icon
* Edit, fix things up
* Then click save, submit a PR, Github will guide you along
 
TripleA is a community supported project. Any help with the documentation is really appreciated. If you follow these steps, pay forward to the next person and fill in missing steps and add clarifications!

