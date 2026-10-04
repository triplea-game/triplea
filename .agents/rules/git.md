# Git and docs

The user makes and publishes all commits personally. Do not run `git commit` or `git push`, do
not stage files, and do not add `Co-Authored-By` lines. Leave changes in the working tree and end
with a short summary the user can turn into a commit message.

Documentation is part of the change: when code changes something an `AGENTS.md` or a file under
`docs/` states (module layout, commands, conventions, behaviour), update that file in the same
turn. The `docs-check` skill does this; `docs-test` checks that links and documented commands
still work.
