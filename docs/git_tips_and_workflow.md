# Git Tips and Workflows
A wiki page to document command line work flows and Git usage tips.

## TODO
- git setup instructions
- how to update

## Tips

### Tools
- SCM breeze is pretty nice CLI helper: https://github.com/ndbroadbent/scm_breeze
  - it provides a number of aliases, for example `gb` will be short for `git branch`, and `gs` for `git status`
- EGit in Eclipse: http://www.eclipse.org/egit/
  - more assistance can be found on http://www.vogella.com/tutorials/EclipseGit/article.html

### FAQ
- Instead of `git pull` do: `git pull --rebase`. If you are adding a merge commit after each time you do a pull, the rebase flag is needed

### Aliases
```
alias gupdate='git fetch upstream && git pull --rebase upstream master'
```

## Working with Remotes
Example system for setting up remotes:
```
$ git remote -v
origin	git@github.com:GitHubUser/triplea.git (fetch)
origin	git@github.com:GitHubUser/triplea.git (push)
upstream	git@github.com:triplea-game/triplea.git (fetch)
upstream	git@github.com:triplea-game/triplea.git (push)
```
Note the 'upstream', 'origin'. If you do not have the 'upstream' one set, add it with:
```
$ git add remote upstream git@github.com:triplea-game/triplea.git
```

## Branches
A note on branches: There are three branches at play:
- master of TripleA repository (lives on github)  (upstream)
- master of your fork of TripleA (lives on github)  (origin)
- master on your local clone of your fork (lives on github)  (local)

## Splicing a PR

Let's say you are working on a branch called 'project'.
Also, let's say you have these three commits:
```
Test Attachment Changes       3d98ce9
Update Attachment Object      879cea8
New Util Functionality        3cd8c65
```

Notice that the last commit has very little to do with the other 3. What you can do is carve off the last commit on to it's own branch, and then submit that for a PR. This then allows a PR for the first 2 commits to be more focused, and easier to review. 

There are several ways to slice it. Before slicing a PR, you need to be sure of two things:
1. You have pushed your branch to the remote. If you mess things up *really* badly, you can get your work back.
2. You need a clean working state. `git status` should show no files as modified

### Via Cherry-Pick

First we will checkout master and update it to the latest of TripleA (upstream) master
```
$ git checkout master
$ git fetch upstream && git pull --rebase upstream master
```

Now we will create a new branch:
```
$ git checkout -b util_function_branch
```

Now cherry-pick the last commit (recall it was `New Util Functionality        3cd8c65`):
```
$ git cherry-pick 3cd8c65
```

Now push the branch:
```
$ git push origin util_function_branch
```
And submit a PR for it.

The other branch can go on with the unique commit in it, git will recognize it as the same commit from the other branches. IF the commit is totally unrelated, then in the other branch you may want to get rid of it. There are a couple of ways to do this:
- You can delete the commit in the other branch by using `git rebase -i HEAD~2` (after which you'll want to run `gupdate`). 
- If the commit is the last one like it is here, you can pull the git commit pointer back by one `git reset --soft HEAD` and then reset and undo the file changes. `git reset <path..to..file>; git checkout <path..to..file>`

### Splicing a PR while working
Let's say you have 5 files all with modifications. Let's say the first 2 are related and the last 3 are related, so we can split it up into two different PRs. Here is how to put the first two files on one branch and the last three on a second:

```
## Assumption, we are on master, it has been updated so our master is even with the upstream master,
## and we have 5 modified files that have not been committed

$ git status  ## should show 5 changed files, none committed
$ git checkout -b first_change_branch
$ git commit <path_to_first_file> <path_to_second_file> -m "Commit Message"
$ git push first_change_branch origin
$ git checkout master
$ git checkout -b second_change_branch
$ git commit <path_to_third_file> <path_to_fourth_file> <path_to_fifth_file> -m "Commit message"
$ git push second_change_branch origin
```

One key is to note that we keep switching back to master.
