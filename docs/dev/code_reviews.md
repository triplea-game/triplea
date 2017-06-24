---
layout: longpage
title: Code Reviews
permalink: /dev_docs/dev/code_reviews/
---

This page describes the roles in a Pull Request (PR), guidelines for what is expected from code reviewers, how to become one, and guidelines of what is expected from someone that submits code.

# Process

### PR Guidelines
- describe functional changes
- describe what was tested
- describe refactoring that was done
- if UI changes were made, include screen shots giving an overview of how things look
- code contributors should test their updates quite thoroughly


### Code reviewer expectation:
- shall not "self-review" and "self-merge" their own work 

### Requirements for someone to have write access
- contribute some code
- provide valuable code reviews
- sustained engagement over at least a few months

# PR Merge Guidelines

At least one code reviewer must:
- try out any functional changes
- do a thorough code check
Before merge:
- all comments questions should be answered/addressed


# Code Review Ordering
- Reviewers should start with the oldest PRs and work their way forward in time. Favor merging things in that order as well. The reason for the merge order is to make merge conflicts a bit more predictable, if you are the first PR in the queue, then there should be no merge conflicts. If 2nd, then in theory you would only have to worry about the one open PR request to cause merge conflicts.

# Tips and Questions to ask when code reviewing
- Does anything *have* to change in the PR for it to be accepted?
- Was the risk of this PR correctly identified and considered?
- Is the overall design reasonable? Are we adding more to the mess than cleaning?
- Does the logic look correct?
- Any major code smells?
- Were unit tests added or updated?
- Without having looked at the code diff, do the commit comments make sense? Once you look at the code diff, were the commit comment still meaningful and complete?


