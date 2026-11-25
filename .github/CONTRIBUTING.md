Contributions are really welcome. See the [dev-setup](/docs/development/README.md)
for more information on how to get started, guidelines, conventions, and
[areas of need](/docs/development/areas-of-need.md)

# Contribution Guidelines

- TripleA is deceptively difficult to work with:
  - quite brittle, easy to break things
  - tangled, lots needs to be done and difficult to do so in bite-sized iterations
  - design is sometimes hasty and primitive
  - non-obvious dependencies, reflection and serialization compatibility can
  break things. For example re-ordering or renaming variables and methods
  can break compatibility between TripleA Versions.

## Testing Responsibility
Each contributor should test their changes thoroughly. We wish to minimize
problems when it comes time for releases. The goal is small, incremental,
and solid changes.

TripleA lacks automated testing and the manual testing overhead is large.
For now the manual testing needs to be done to ensure we keep a solid
codebase going forward.

Automated tests are hugely welcome, TripleA needs lots more so that the
manual testing can be reduced and changes made with confidence.

## Things to watch out for - Compatibility Breakers
There are three ares to be concerned about compatibility:
1. Save game serialization. Elements of `GameData.java` are serialized to a file
and then loaded again. Private variable changes here can break things. To test this,
save a game file, make changes, then verify that the save game can be loaded.
1. Network: a custom variant of RMI (remote method invocation) is used. To test
this, start a game without any changes, host. Then in another instance, make
changes, launch, and connect to the host and play the game through to a combat round.
That should test most functionality to verify network compatibility
1. Map XML loading: most of this is now explicit and is less brittle. After making updates,
load several maps to verify that map XML 'attachments' can be mapped to game code without
problems.

# Submitting a PR and Code Review
- See the [Compile and Launch section of the Readme](/docs/development/README.md#compile-and-launch-triplea-cli) to see how
run verification and tests locally.
- Typically most PRs will not be reviewed until they pass the travis CI build.
 Comment in your PR if you wish for a preview review to be done to get high level feedback.
- UI changes should ideally include screenshots, this helps the larger community preview
the changes, lots of non-developers are TripleA experts and can use this to provide helpful feedback.
The screenshots help technical reviewers know what has been changed.
- Favor adding automated testing, it's a key missing component to TripleA, getting our test
coverage up is critical for us to move quickly in TripleA, and we are a long ways away and have
too much to do and spend too much time on overhead and manual testing.
- Keep changes focused, try to have one purpose behind any PR. For example split bug fixes from refactoring
- Favor PRs that are short and easy to review, under two hundred lines
- PRs should not stay open for a long time, if they are not merge-ready, or will not be within a few
days, close and re-open them when they are ready.

### PR Merge Guidelines
At least one code reviewer must:
- try out any functional changes
- do a thorough code check
Before merge:
- all comments questions should be answered/addressed

### Code Review Ordering
- Reviewers should start with the oldest PRs and work their way forward in time.
 We will favor merging oldest PRs to last to keep merge conflicts more predictable.
