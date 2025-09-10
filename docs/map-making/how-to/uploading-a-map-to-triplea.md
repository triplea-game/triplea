- [New Map Development](#new-map-development)
  - [Posting to Github](#posting-to-github)
- [More about triplea_maps.yaml](#more-about-triplea_mapsyaml)
- [Github repo change ownership option](#github-repo-change-ownership-option)

Maps are hosted in [github triplea-maps](https://github.com/triplea-maps/) and found by the game engine through a property file, [triplea_maps.yaml](https://github.com/triplea-game/triplea/blob/master/triplea_maps.yaml), that is read live every time the game starts.

Maps can be hosted at any time, before they are complete. Map repositories provide a download link that will zip a map up for you, this can be an easy way to share maps that are still being worked on.

## New Map Development

* Perhaps the biggest trick is to get get the folder structure right.

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
**If the upload size is too large, do it a few files/folders at a time or use 'Github Desktop' or 'Git for Windows' or 'SourceTree' to do it in bulk.**
Note, the files and folders uploaded should be unzipped. Zips are for download only convenience. When working with a map, you can and should have it unzipped, and same for upload.

## More about triplea_maps.yaml

* Map description and download links are pulled from: https://github.com/triplea-game/triplea/blob/master/triplea_maps.yaml
* Please submit a PR to that file to include/update maps.

The version field indicates to the game engine when a map should be marked as out of date for download purposes:
* To indicate in game that your map should be redownloaded, update the version number in the triplea_maps.yaml file.

## Github repo change ownership option

Create a repository in github. Use the same general layout as the other map repositories. Stage your map repo from the 'downloadedMaps' folder, here the game engine will pick it up and you can test it out and update it. Check it all in, and push it to your repository. Then in the github repository settings, look for the 'change ownership' option, and you can request for the 'triplea-maps' org to take ownership. After ownership has been transferred we would add in a ".travis" file to enable automated zipping, and we would update: https://github.com/triplea-maps/Project/blob/master/production_config/triplea_maps.yaml, to make the map live

