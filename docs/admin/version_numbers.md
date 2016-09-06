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
