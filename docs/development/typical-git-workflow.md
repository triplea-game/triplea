# Git Help and Typical Commands

First, fork the triplea-game repo and clone your forked repo. By default the cloned
repo folder will be 'triplea'.

## (one-time) Add TripleA main repository 'upstream' as a git remote

```
cd triplea/
git remote add upstream git@github.com:triplea-game/triplea.git
```

## Typical workflow

```
cd triplea/
git checkout master

## Refresh to latest code
# Update local 'master' with the latest code from the main project
git pull --rebase upstream master/

# Update 'fork' repository with latest 'master'
git push origin master/

## Start feature branch work flow, create a branch
git checkout -b "my-feature-branch-name"

## do work
git add <new-files>
git commit .
## Enter in a commit message and save,
## Double check files committed message looks good.

# push the branch to your fork
git push origin
## Check the output, look for the 'create PR' link
## Follow the 'create PR' web link
```

Read more about TripleA's  [pull requests process here](../project/pull-requests.md).
