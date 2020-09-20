# Developer Setup Guide

* Download an IDE (IDEA is preferred but Eclipse is okay too!)
  and [setup plugins and formatter](./ide-setup)

* Create a fork of triplea-game/triplea

* Clone your fork locally (be sure you are setup on github and 
  have an SSH key configured).
  
* Import the project into IDE, there are launchers checked-in to
  launch a game client
  
* Install Docker

  
## Running tests

Assuming docker is installed and a mac or linux system, first start database:
```
cd triplea/database/
./start-database
```

Next run verify script:
```
cd triplea/
./verify
```

## Testing a local lobby

* Start database. If you just ran tests, reset the data:
```
cd ./database
./restet_docker_db
```
* Start lobby, look for the lobby server launcher
* Start a bot, look for the headless game launcher
* Start the client, look for headed-client launcher
* In engine settings, go to test, select "use local lobby"
* Click play online, log in without an account or use the predefined
  moderator account (user:password) "test:test"


## Add TripleA main repository 'upstream' as a git remote 

One-time, add the upstream remote

```
cd triplea/
git remote add upstream git@github.com:triplea-game/triplea.git
```

## Typical workflow

```
cd triplea/
git checkout master
git pull --rebase upstream master/

## This next step is option to keep your remote 'master' branch in-sync
git push origin master/

git checkout -b "my-feature-branch-name"

## do work
git add <new-files>
git commit . 
## enter in a commit message
git push origin
```

With the above done, you'll get a link in the push output that can
create a PR.

## PR workflow

* Before creating a PR, do a self-review of the code that it looks good
* Ensure that your commit message is descriptive. Remember the commit
message is for future developers and maintainers, any text in the PR
is one-time and is for reviewers for the benefit of doing review. The
commit message should describe roughly what changes and most importantly
why it was changed.
* Favor prefixing the title of the commit message with one of these keywords:
  * Fix
  * Update
  * Add
  * Documentation

The project maintainers will try to leave some sort of comment within 48 hours
and give an expectation when a more detailed review can be done.

## Tips for Successful PR

Start more significant changes by submitting an issue and talk through what
you plan to change. This allows us to discuss it and align our plans to
make sure they'll fit. This prevents surprises for everyone when it comes
to review.

If making significant changes to the UI, create a thread in the forums
to also get non-developer feedback and to reach a larger audience.

Try to submit in chunks of under 500 lines at a time. 

It can be really difficult to break up PRs into reviewable chunks. Use
the `ClientSetting.isBetaFeature` flag to disable in-development
code.

Add comments to your own PR to indicate where more work will be done.

Be open to suggestion and remember we are stretched thing and will likely
review too quickly. We'll try to make it clear when something *has* to change,
otherwise everything is all just a suggestion.

Try to keep PRs single focus, change mainly just one theme of thing at a time.

Use commits to break up changes for ease of review. All commits are squashed
together on merge.

## Pain Points to be aware of

* TripleA is VERY brittle, be really careful and test your changes. We like to
  see good automated tests as much as possible to help us going forward and
  reduce the amount of we have to test. Try to make sure your changes are super
  solid so we can be confident every change is a step forward and we are not
  spending lots of time debugging obsure errors when it comes to release.

* TripleA seems simple at first, but it's very much tangled in knots in
  many places. To fix one thing, you often need to fix another, and to fix
  that you need to fix perhaps the first thing and something else. 
  
* Save game and/or network compatibility can be quite easy to break,
  be aware of how serialization is done for save games. Unless we are in
  a major release cycle, keeping compatibility can be really constraining.
 
 
 ## Why Work on TripleA?
 
 * If you like the game and want to improve it
 * If you want to gain skill working with a legacy code base
  (refactoring, testing, safely making changes, techniques for migrating code)
 * To get practice coding and working within an open source team
 * To get better at specific technologies that are a fit and in-use by TripleA.
 
 If your goals align with those of TripleA, then this can be a really rewarding
 project! We will not lie about the frustration you can run into, and it's not
 easy, you will need to persevere and in the process will potentially grow and
 learn a lot.
