## Items to Include in a Pull Request
- Description of what changed and why, this should come auto-filled from the commit message
- How to test
- Description of what refactoring was done, calling out as well what refactoring was not done


## Questions to ask when code reviewing
- Does anything *have* to change in the PR for it to be accepted?
- Was the risk of this PR correctly identified and considered?
- Is the overall design reasonable? Are we adding more to the mess than cleaning?
- Does the logic look correct?
- Any major code smells?
- Were unit tests added or updated?
- Without having looked at the code diff, does the commit comment make sense? Once you look at the code diff, was the commit comment still meaningful and complete?

## Code Review Process / Workflow
Let's say for example a reviewer has 20 minutes they can spend towards review. They would open up the pull request queue, and start at the oldest. There are then only a couple of scenarios and responses they have to consider as they work their way up the queue.

Scenarios:

#### PR is waiting for feedback from the author
Move on

#### PR is large and has yet to be looked at
Skim it over in 2 or 3 minutes. See if there is anything obviously wrong or any other quick initial feedback you can give the author. If there is anything you spot that needs to change, comment what and close the PR. Otherwise, comment that you'll return later to finish the review.

#### PR is large and has been skipped over once already
If the remainder of the 20 minutes is enough to finish reviewing it, then go for it, otherwise move on

#### PR is small and has yet to be looked at
Review it, or if you only have a couple of minutes left, come back later and start over.

#### PR is small and has already been passed over once
This should ideally not happen, but if it does, review it if you have time.

