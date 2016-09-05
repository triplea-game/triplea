### Create fork of tripleA repo on github
Done via github.com, click the "fork" button: https://github.com/triplea-game/triplea

### Create a clone of your Fork
```
$ git clone git@github.com:JaneDoe/triplea.git
$ git clone git@github.com:JaneDoe/triplea.git folderToCloneIntoName
```

### Add tripleA repository as an upstream repository
```
$ git add remote upstream https://github.com/triplea-game/triplea.git
$ git remote -v
origin	git@github.com:JaneDoe/triplea.git (fetch)
origin	git@github.com:JaneDoe/triplea.git (push)
upstream	https://github.com/triplea-game/triplea.git (fetch)
upstream	https://github.com/triplea-game/triplea.git (push)
```
### updating locally from your fork
`$ git pull --rebase`


### syncing your fork with the upstream tripleA repository
```
$ git checkout master

$ git fetch upstream
$ git pull --rebase upstream master

$ git fetch origin
$ git pull --rebase

$ git push origin master
```


### how to resolve merge conflicts
Also you'll need a mergetool. Opendiff is a nice one for mac, meld is working well for me on linux. You use the mergetool via:

`$ git mergetool <file_in_conflict>`
 Now use the merge tool to resolve the conflict. You should have a LHS, RHS, and a third view that shows what the output file will look like. Once done, save, and quite

```
$ git add <file_in_conflict>
$ git commit
```
   Note, it is important to not have an argument to this commit, you're telling git that you've done all merge resolutions`

### cherry pick commits between branches
`$ git cherry-pick *commit sha*`

git finds the commit identified by the SHA passed in, which can be on any branch, and applies that commit to the local branch. 


### revert files
`$ git checkout $file`

Works on folders too. More formally speaking it sets the file or folder (and all children, ie: recursive) back to the current branch (removing modifications, but does not untracked delete files)


### Branching and pushing to origin (the repository fork)

| Git Command |What it does|
|---|---|
| $ git branch | shows current branch |
| $ git checkout -b *myNewBranchName*  | creates a new branch |
| $ git checkout -m *renamed_branch* | renames current branch |
| $ git push origin renamed_branch  | pushes branch to origin (your fork on github) |
| $ git push origin :renamed_branch  | deletes branch on origin (does not impact local, remote only) |
| $ git branch -d renamed_branch  | deletes branch, local only |
| $ git branch -D renamed_branch  | deletes branch, local only, -D forces deletion if there are commits on the branch not yet merged |
|---|---|


