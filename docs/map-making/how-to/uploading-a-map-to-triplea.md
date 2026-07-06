- [Making your map downloadable via TripleA](#making-your-map-downloadable-via-triplea)
- [Updating your map](#updating-your-map)

# Making your map downloadable via TripleA
This guide explains how to make your map downloadable via TripleA. After having followed these steps, your map will appear in the "Download Maps" list in TripleA, as well as at https://triplea-game.org/maps-list/maps/. Make sure that before you follow this guide, your map is **working** and ready for upload (no obvious bugs, missing files, etc.). If you missed some small bugs, that is no big deal. You can always fix bugs or make changes to your maps later on. How to do that is explained at the end of this guide.

The whole process can be summed up in these steps:
1. Check the file structure of your map
2. Sign up at GitHub
3. Create your own map repository at GitHub
4. Download and install Git
5. Use Git GUI to upload your map
6. Request your map to be added to the official TripleA map repository

## 1. Check the file structure of your map
If you have just worked on your map, it is presumably in your downloadedMaps folder. Make sure your map's folder name is in lower case letters and with underscores between words (e.g. "my_map"). The file structure inside this folder is important. It should contain the following:

<img width="640" height="182" alt="image" src="https://github.com/user-attachments/assets/c2399fd2-54ad-4d73-9fc2-57ba252dbe91" />

* Folder called "description", that contains a small PNG file of the game board (around ~400 px wide). The name of this file should be "TripleA_[map folder's name]_mini.png" (e.g. "TripleA_my_map_mini.png").
* Folder called "map" that contains all your relevant game files (baseTiles, flags, games, centers.txt, place.txt, etc.).
* "description.html" file, that contains a small description about the game. Important things to include here are: scenario, time period, setting, etc. Example, "World War II in Europe, starting in 1942". This is the description that people will see when they scroll through the list of maps, so make sure the description clearly tells what the game is about and possibly the captivates players to play your map!
* "preview.png" file, should be the same image that's inside the "description" folder, except that it is called "preview.png".
* "map.yml" file. This file specifies important information about your game. "map_name" should be the name of your map in lower case letters and with underscores. "game_name" is how you want the name of your game to appear in the TripleA "Select Game" list. "file_name" is the name of the XML file used by your game. If you have a game that has multiple scenarios with multiple XMLs, you can list multiple instances. See screenshot below.

<img width="414" height="62" alt="image" src="https://github.com/user-attachments/assets/a764f9a5-937c-4282-a642-0984204b5736" />

If you need to know how the inside of the folder called "map" should look, consult the following tutorial: https://github.com/triplea-game/triplea/blob/main/docs/map-making/tutorial/creating-custom-map-xml.md

## 2. Sign up at GitHub
Next, you should create an account at GitHub, the place where TripleA maps are stored and the game is developed: https://github.com/.

## 3. Create your own map repository at GitHub
You must create a new repository at GitHub where you can upload your map to. Name the repository after your map’s name, and remember to use lower case letters only and underscores to separate words.
Click on your GitHub profile picture (top right corner of GitHub) - click *Repositories* - click *New*. Fill in the fields as shown in this example: 

<img width="1919" height="1000" alt="image" src="https://github.com/user-attachments/assets/5840e2ae-151e-4d6d-9e3d-3572ca0343aa" />

## 4. Download and install Git
You will need the program that can upload your map to your GitHub page. Get it here: https://git-scm.com/downloads
Install it with all default options if you don’t know what you are doing.

## 5. Use Git GUI to upload your map
*	Start the Git GUI program.
*	Click *Create New Repository*.
*	Find the place on your computer where your map is and click *Create*. Example: C:\Users\VictoryFirst\triplea\downloadedMaps\my_map
*	Now the program starts up. The next step is to let the installed program be recognized and trusted by GitHub as your computer, when you try to upload the map to GitHub. For this we need to copy an “SSH key” from the installed Git GUI program and paste it into your GitHub settings. In Git GUI click *Help* - click *Show SSH Key* - click *Generate Key* -  Type nothing, just click OK, and OK again - Copy the new key to clipboard and close.
*	Go to Github.com - click on your profile icon - click *Settings* - click *SSH and DGP keys* - click *New SSH Key* - In first field type something like “Git GUI on my PC” - In second field paste the SSH key from your clipboard and click *Add SSH key*. GitHub will probably send you a mail informing you about this added key.
*	We need to give Git GUI on your PC some information that can become your signature when you upload to GitHub. In Git GUI click *Edit* - click *Options* - enter your GitHub username and email adress under *Global (All Repositories)* - click *Save*.
*	Before we can upload to Git GUI, we also need to inform Git GUI what remote location we want to upload to. Go to your GitHub repository, click on the green button that says "Code" and copy the link. This is the repository adress. In Git GUI, click *Remote* - click *Add...* - in Name, type in an identifying name - in Location, paste the repository address, select Do Nothing Else Now, and click Add.

<img width="544" height="390" alt="image" src="https://github.com/user-attachments/assets/f1391e5e-6bee-4a05-92a7-9a65ce4eeff6" />

*	Now you can upload your map. In Git GUI, press *Rescan*.
*	Press *Stage Changed* (Press Yes and let the program work. Some warnings might pop up, like “LF” to “CRLF ending replacements. Just choose *Continue*.).
*	Press *Sign Off* (This auto generates a signature text based on the information you have written in options and pastes it into the box to the right. You can delete this information and type something else like "Initial Release" or "Version 1.0" if you want.).
*	Press *Commit*
*	Press *Push* - In the new window, select your local map repository (it is probably already selected) - Click *Push*. If everything is good, expect the program to show “Working… please wait…”. It can take some time, depending on your map size. When the map is uploaded, you will get a green Success notification.
*	After upload, you can go to your personal GitHub page and inspect the upload. Does the new specific map repository now contain a folder called map? And does this folder contain all your map files? If so, everything is alright.

<img width="894" height="487" alt="image" src="https://github.com/user-attachments/assets/8475aa86-ec7c-4493-98bb-97269f8f141d" />

## 6. Request your map to be added to the official TripleA map repository
Your map then needs to be added to TripleA's official map repository, called "triplea-maps". You cannot do this on your own. Instead, you need to ask someone with permissions to do this for you. To do this, you need to create an issue in the TripleA GitHub.
Go to https://github.com/triplea-game/triplea. Click on "Issues", "New Issue" and then choose "Map Registration". Fill out the fields as shown in the screenshot below and wait until a map admin adds your repository to the TripleA maps repository.
   
<img width="790" height="696" alt="image" src="https://github.com/user-attachments/assets/8eea2850-f337-4010-be64-d3f395a0a76b" />

Once that has happened, your map can now be downloaded via TripleA!

# Updating your map
If at some point you would like to update the map you uploaded, execute the following steps:
* Go to the repository of your game that is in triplea-maps. IMPORTANT: this is **not** the personal repository that you created during step 3. To find it, go to: https://github.com/triplea-maps and search for your map.
* Drag and drop the new/changed files to your repository. Make sure they are uploaded to the correct subfolder. You can also edit files directly by opening the file in GitHub and then selecting the pencil icon to the right of the screen. You can also delete files by clicking on them, then on the three dots on the right of the screen and then on *Delete file*. It is recommended to change the version number in the XML and game notes when making changes to your map.
* If you don't have write access to your repository, any changes you made will turn into pull requests. They then need to be merged by the person who has write access (this is usually the person who uploaded your map to triplea-maps). Wait for them to merge your pull request.
Once the pull request is merged, your changes should be immediately live. Players that have your map installed will get a in-game notification, telling them that their map is out-of-date. They can then install the latest version of your map.
