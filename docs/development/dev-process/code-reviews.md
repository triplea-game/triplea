# Code Reviews

Most all changes should go through a code review. Code review should focus on value provided and verifying design is reasonably easy to understand and clean within the intended scope of the update.

- PR authors are responsible for achieving consensus on updates. It's recommended to post to forums about upcoming changes to help begin that conversation.
- PR reviewers should be explicit when a PR fails to hit the approval bar and try to ensure the last comment is a list of action items the author would need to satisfy for merge. Reviewers should, as best they can, work with PR author to recommend and come to compromise solutions.

Code is reviewed in the order it is submitted, older PRs before newer PRs.

Code is approved and merged if the answer is yes to the following questions:
- Is the code somewhat understandable? (ideally very understandable, some code is a real mess and 'somewhat' is about the best we can hope for within reason).
- Is the needed documentation captured in code or when appropriate the commit comment?
- Are all coding conventions followed?
- Are class, package, database names all well named?
- If there are existing tests, were they updated to include the new changes?
- Is the update well tested (either manually, but preferably automated)?
- Is the update complete, or any left over items well indicated? For example, javadoc comments should reflect all changes.

Code is commented on but still approved if:
- Could local variables or methods be slightly improved in their naming?
- Could the code syntax be written in a slightly cleaner way.

PR authors are generally expected to make updates to respond to comments or to comment if something is not being done. Reviewers should be careful to keep comments to within the scope of the intended changes and allow for follow-ups post merge.

PR authors should focus on code correctness, bug free updates, clean design. TripleA is a BRITTLE code base, every release sees subtle bugs. The goal is fast iterations with well made changes that do not introduce problems. An accumulation of bugs before release has been historically a major problem.
