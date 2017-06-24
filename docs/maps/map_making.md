---
layout: longpage
title: Map Maker Documentation
permalink: /dev_docs/maps/map_making
---


## How to Request Map XML Updates
* This can be done using just your web browser:
  * Find the github map repo of the map you would like to update: https://github.com/triplea-maps
  * Click through the maps/games folder
  * Now you can edit the XML file, click the file name, and follow these instructions: http://help.github.com/articles/editing-files-in-your-repository
* When saving, github will guide you through for creating a fork,  which is a remote copy that you own. From that you can request changes in your 'fork' to be 'merged' back in to the main map repository.
* This will lead to the creation of a pull request. Map admins and or map owners will review the change and will then either comment on it requesting clarification or updates, or they will merge it.


## Adding or replacing files
* Similar to updating files, the github website will allow you to do this. To do it in bulk you may need to do some git. Github desktop from github is a pretty easy tool, there is also Git for Windows, source tree which are other good git clients.


## New Map Development

* Perhaps teh biggest trick is to get get the folder structure right.

* You can develop your map directly from the "downloadedFolders" map and the game engine will pick it up. Simply create your map folder there wth a 'map' folder, and then place the various map files underneath. For example, your "downloadedFolders" might look like this:  ![above_the_project](https://cloud.githubusercontent.com/assets/12397753/17640925/f50e1876-60c0-11e6-96d8-483f0a84f389.png)

* In the above picture, "big_world_2" is the folder we are working on, after creating a similar folder, the next level should contain a "map" folder. Any files located at this level are support. A typical config will look like this:
![bare_bones_top_level](https://cloud.githubusercontent.com/assets/12397753/17640936/30528e44-60c1-11e6-815e-e03c395a26b5.png)
or like this:
![top_level](https://cloud.githubusercontent.com/assets/12397753/17640928/f9269118-60c0-11e6-84b5-63a0153ed4fb.png)

* The last level contains the standard map folders and files, should look something like this: ![3rd_level](https://cloud.githubusercontent.com/assets/12397753/17640896/010137cc-60c0-11e6-8f02-4700c709ab66.png)


### Posting to Github
* Create a github account
* Create a repository
* Upload your map folder to it
** If the upload size is too large, do it a few files/folders at a time or use 'Github Desktop' or 'Git for Windows' or 'SourceTree' to do it in bulk.
** Note, the files and folders uploaded should be unzipped. Zips are for download only convenience. When working with a map, you can and should have it unzipped, and same for upload.

## How are maps included with the game?

* Map description and download links are pulled from: https://github.com/triplea-game/triplea/blob/master/triplea_maps.yaml
* Please submit a PR to that file to include/update maps.

The version field indicates to the game engine when a map should be marked as out of date for download purposes:
* To indicate in game that your map should be redownloaded, update the version number in the triplea_maps.yaml file.


## How to play a map pre-release
Three ways:
1. If the map is officially part of the maps project, then look for the releases section of the map, and then look for the zip file download link. Example:
* https://github.com/triplea-maps/world_war_ii_g40_balanced/
* https://github.com/triplea-maps/world_war_ii_g40_balanced/releases

2. You can also go to the main page of the map, and click "clone or download", and save the map repo as a zip. Then extract the contents of that zip into the triplea 'downloadedMaps' folder (you can set this path from within the game via the settings window)

3. You can do a 'git' clone of the map repo directly to 'downloadedMaps'



## Github repo change ownership option

Create a repository in github. Use the same general layout as the other map repositories. Stage your map repo from the 'downloadedMaps' folder, here the game engine will pick it up and you can test it out and update it. Check it all in, and push it to your repository. Then in the github repository settings, look for the 'change ownership' option, and you can request for the 'triplea-maps' org to take ownership. After ownership has been transferred we would add in a ".travis" file to enable automated zipping, and we would update: https://github.com/triplea-maps/Project/blob/master/production_config/triplea_maps.yaml, to make the map live


## Getting More Help
- Ask questions here: github.com/triplea-maps/Project/issues/new
- Or Jump on gitter: https://gitter.im/triplea-maps/Project
  - You can chat there about maps, map making. And also you'll hear announcements as well, and the general community chatter. If you drop off for some time, for a few weeks or a few months, then you'll need to review the docs a bit to see what you missed. At least, that is the intent. So if you have any questions, where you want to reach map makers and the map admins, you can ask them on gitter, and chances are decent they'll be answered within hours or a day.



## Volunteers needed!

This documentation is rough, and could use some help. Contributions to help improve it would be really useful for the community.


# Legacy

## Map Releases and Automated Zipping
Legacy note: we do not need travis to zip the map files if we are using the repository zip file available with each github repository automatically (https://github.com/triplea-game/triplea/issues/1160). We still have the travis builds in place (2016-09-18), but they may be removed since we are no longer using the artifacts produced by them.


* We are integrated with Travis that will watch map repositories for updates. Whenever a map repository is updated, Travis will kick and will version the map, and publish a map zip to the '/releases' section of the map repo. For example: https://github.com/triplea-maps/age_of_tribes/releases
* You should find that you no longer have to do any map zipping or unzipping.
