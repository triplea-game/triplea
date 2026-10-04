---
paths:
  - "**/*.sh"
  - ".build/**"
  - "verify"
---

# Shell script rules

- Bash, starting with `#!/bin/bash` and `set -eu`.
- `$(...)`, never backticks. Scripts must pass shellcheck.
- `cd` only inside a subshell: `(cd dir; ./do_something)`.
- Outermost variables are CAPS and `readonly`; function locals are `local -r lowerCamelCase`.
- Function parameters are assigned to named locals in the first lines of the function.
- Long scripts are split into functions with a `main` called at the bottom.
