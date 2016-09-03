### Install Dev Environment Requirements

#### Github account: 
go to github.com and create an account. (strong recommendation to set up two factor authentication so that your account will remain secure)

#### Local Install:
- Git
- Gradle (2.0+)
- eclipse or intellij
- something to run Git commands with if on windows (examples: cygwin, tortoiseGit, sourcetree)

#### Git Configuration:
- set up git author name and email
- set up passwordless github access. Essentially you'll generate a private and public key, and you'll upload the public key to github.
  - https://github.com/saga-project/BigJob/wiki/Configuration-of-SSH-for-Password-less-Authentication
  - TODO: how does this change for windows? Add notes if there are any differences or remove this todo if none.

### Fork the 'main' tripleA repostory
Go here: https://github.com/triplea-game/triplea, click the "fork" button in the top right

### Clone
On your new repository page, there will be a link to clone your repo on the right. If the link begins with "https" you'll be prompted for passwords. If you set up passwordless, there will be a ssh option and the link will start with "git@.." (this is preferred, more convenient and secure)

Go to your local git environment and run "git clone" and then the URL copied from git.


## Building

Run this gradle command from the project root:
$ gradle assemble


## IDE Setup

### IntelliJ
`<placeholder, please fill me in>`
`<placeholder for setting the code style formatter>`

### Eclipse
Workspace one folder above where you cloned triplea
- new project "triplea"
- fix build folders, src has an "excluded" in it:
-- build path should just be 'src' and 'test', and everything in them with no exclusion
- all jars in lib folder should be on classpath, likely picked up automatically
`<placeholder for setting the code style formatter>`


## IDE: Running tripleA and tests

Main game launcher is "GameRunner.java". When running, you need the following folders on the classpath:
- maps
- assets

Same for when running tests.
