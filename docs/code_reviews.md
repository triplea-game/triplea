This page describes the roles in a Pull Request (PR), guidelines for what is expected from code reviewers, how to become one, and guidelines of what is expected from someone that submits code.

# Process

## Roles
### Code contributor:
- will be just that, the person that submits a PR.
- a code contributor will be expected to do 99% of the work in a PR. That is purely to keep overhead low, we do not have payed full time people working on the project whose job is to do reviews. Thus, if there is a choice between a reviewer or a contributor doing work, the choice will be for the contributor to do it. This is to decentralize the work as much as possible.
- code contributors should test their work quite thoroughly 

### PR Guidelines for a code contributor
- describe functional changes
- describe how to test the change
- describe refactoring that was done
- if UI changes were made, include screen shots giving an overview of how things look

### Code reviewer role:
- would have write access
- be responsible for shepherding PRs, and hitting the merge button at the appropriate time.
- reviewers will do functional reviews when functionality has changed. The functional review will generally be pretty brief, mostly a smoke test to make things work as described, and also look good to someone else who did not author the original code. To facilitate this, please be as clear as possible when submitting PRs as how they can be tested, which areas of functionality were updated, make it easy for 

### Requirements for someone to become a code reviewer
- contribute some code
- provide valuable code reviews
- sustained engagement over at least a few months

# PR Merge Guidelines

At least one code reviewer must:
- try out any functional changes
- do a thorough code check
Before merge:
- all comments questions should be answered/addressed

Avoid self merges:
- contributors of a PR should not be considered as reviewers when it comes to their own PRs
- the reasoning for self merges should always be explicitly indicated
- The following are the exceptions to no self merges:
 - things that are more urgent, people waiting for a bug fix, people waiting to test
 - build process items
 - things that are just much easier to test when merged to master  (for example they only kick in when merging to master as well)

# Review Guidelines

## Code Submitter
- Focus on code updates first to respond to PR commentary. In places where there is disagreement, see if you can first update the code to achieve whatever possible common ground you might be able to find. Then for anything remaining, discuss why the code is good enough left in the current state. 

## Code Reviewer
- Mention both positive and negatives, not just negatives
- Avoid line by line nitpicking, instead summarize
