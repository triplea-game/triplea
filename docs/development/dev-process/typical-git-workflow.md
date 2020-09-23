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

## This next step is optional to keep your remote 'master' branch in-sync
git push origin master/

git checkout -b "my-feature-branch-name"

## do work
git add <new-files>
git commit . 
## enter in a commit message
git push origin
```

With the above done, you'll get a link in the push output that can
create a PR. Read more about [pull requests here](./pull-requests.md).
