## PR workflow

* Before creating a PR, do a self-review of the code and ensure that it looks good.
* Ensure that your commit message is descriptive. Remember, the commit
message is for future developers and maintainers.  Any text in the PR
is one-time and is for reviewers for the benefit of doing their review. The
PR message (and the commit messages of its contained commits) should describe roughly what changes you made and, more importantly,
why you made those changes.
* Favor prefixing the title of the commit message with one of these keywords:
  * Fix
  * Update
  * Add
  * Documentation
* Create PR with respective message

Detailed PR message
```bash
gh pr create --base master --title "Fix issue #1234" --body "Adds exception handling:\n- Adds null check\n- Adds tests"
```

PR message same as last commit message
```bash
gh pr create --base master --fill
```

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

Be open to suggestions and remember, we are stretched thin and will likely
review too quickly. We'll try to make it clear when something *has* to change,
otherwise everything is all just a suggestion.

Try to keep PRs single focus, change mainly just one theme of thing at a time.

Use commits to break up changes for ease of review. All commits are squashed
together on merge.

