## Workflows

Inside this folder are YML config files that define "[github-actions](https://github.com/features/actions)".

We have the workflows set up so that we only build the modules that have been updated. This is done using the
'paths' attribute within the workflow file.

The workflow files are organized by module. There are three types of workflow file:

(1) Build on merge

(2) Build pull request

(3) Run on demand


## Workflow Types

### Build on Merge

This is a build of the 'master' branch, and is a build that is triggered after we merge a PR. Generally we will
run all tests and then create new distribution artifacts (eg: game installers) and will publish those artifacts.

It is intended for server-side changes that we can get to a place where we automatically update production.

### Build pull request

This type of build is executed against PR branches and should run all compilation and tests but will not generate
nor publish any artifacts.

### Run on demand

This workflow executes an action.. This could be something like having all bots update their maps, deploy SSH keys,
etc.. This type of workflow is useful when there is something that we would want to run, or re-run, without having
to do a full build.


## Workflow file Naming convention

The workflow files are named after the sub-module that they build. If we run all of the same actions for pull
request as we do after merging to master, there will be one file, ie:
```
{module-name}.yml
```

In some cases we have different actions for what we run against a pull request compared to merge, in this case
we have two files:
```
{module-name}-merge.yml
{module-name}-pull-request.yml
```


For workflows that are only run on demand, they are prefixed with the word 'run', eg:

```
run-update-something.yml
```



